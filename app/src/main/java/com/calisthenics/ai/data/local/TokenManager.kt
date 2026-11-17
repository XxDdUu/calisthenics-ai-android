package com.calisthenics.ai.data.local

import android.content.Context
import android.util.Base64
import com.calisthenics.ai.domain.model.SavedAccount
import com.google.gson.JsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class TokenManager @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val keyStore: KeyStore by lazy { KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) } }
    private val generationCounter = AtomicLong(0L)
    private val _sessionState = MutableStateFlow(readCurrentSessionState())
    open val sessionState: StateFlow<AccountSession> = _sessionState.asStateFlow()

    open fun currentSessionSnapshot(): AccountSession = _sessionState.value

    private fun readCurrentSessionState(): AccountSession {
        val activeId = getActiveUserId()
        val hasToken = activeId != null && activeId > 0L && !getAccessToken().isNullOrBlank()
        val resolvedUserId = if (hasToken) activeId else activeId?.takeIf { it > 0L && preferences.contains(KEY_ACTIVE_USER_ID) }
        val effectiveUserId = resolvedUserId?.takeIf { it > 0L }
        val role = if (effectiveUserId != null) getUserRole() else null
        val gen = if (generationCounter.get() == 0L && effectiveUserId != null) {
            generationCounter.incrementAndGet()
        } else {
            generationCounter.get()
        }
        return AccountSession(
            activeUserId = effectiveUserId,
            role = role,
            generation = gen
        )
    }

    private fun updateSessionState(
        activeUserId: Long?,
        role: String?,
        forceBumpGeneration: Boolean = false
    ) {
        val previous = _sessionState.value
        val normalizedUserId = activeUserId?.takeIf { it > 0L }
        val normalizedRole = if (normalizedUserId != null) role?.takeIf { it in SUPPORTED_ROLES } else null
        val userChanged = previous.activeUserId != normalizedUserId
        val nextGen = if (forceBumpGeneration || userChanged) {
            generationCounter.incrementAndGet()
        } else {
            previous.generation
        }
        _sessionState.value = AccountSession(
            activeUserId = normalizedUserId,
            role = normalizedRole,
            generation = nextGen
        )
    }

    open fun saveAuthToken(
        accessToken: String,
        refreshToken: String,
        userId: Long? = null,
        role: String? = null,
        email: String? = null,
        fullName: String? = null,
        avatarUrl: String? = null
    ) {
        val claimUserId = extractClaimFromToken(accessToken, "sub")?.toLongOrNull()?.takeIf { it > 0L }
        val resolvedUserId = userId?.takeIf { it > 0L }
            ?: claimUserId
            ?: getUserId()
            ?: NO_USER_ID

        val resolvedEmail = email?.takeIf { it.isNotBlank() }
            ?: extractClaimFromToken(accessToken, "email")
            ?: preferences.getString(keyAccountEmail(resolvedUserId), null)
            ?: if (resolvedUserId != NO_USER_ID) "user_$resolvedUserId@calisthenics.ai" else "user@calisthenics.ai"

        val resolvedFullName = fullName?.takeIf { it.isNotBlank() }
            ?: extractClaimFromToken(accessToken, "fullName")
            ?: preferences.getString(keyAccountFullName(resolvedUserId), null)
            ?: resolvedEmail

        val resolvedRole = role?.takeIf { it in SUPPORTED_ROLES }
            ?: extractRoleFromToken(accessToken)
            ?: preferences.getString(keyAccountRole(resolvedUserId), null)
            ?: "ROLE_USER"

        val encryptedAccess = encrypt(accessToken)
        val encryptedRefresh = encrypt(refreshToken)
        val now = System.currentTimeMillis()

        val editor = preferences.edit()

        // 1. Mirror into legacy keys for backward compatibility
        editor.putString(KEY_ACCESS_TOKEN, encryptedAccess)
            .putString(KEY_REFRESH_TOKEN, encryptedRefresh)
            .putLong(KEY_USER_ID, resolvedUserId)

        if (resolvedRole in SUPPORTED_ROLES) {
            editor.putString(KEY_USER_ROLE, resolvedRole)
        } else {
            editor.remove(KEY_USER_ROLE)
        }

        // 2. Multi-account storage
        if (resolvedUserId > 0L) {
            editor.putLong(KEY_ACTIVE_USER_ID, resolvedUserId)
                .putString(keyAccountAccessToken(resolvedUserId), encryptedAccess)
                .putString(keyAccountRefreshToken(resolvedUserId), encryptedRefresh)
                .putLong(keyAccountUserId(resolvedUserId), resolvedUserId)
                .putString(keyAccountEmail(resolvedUserId), resolvedEmail)
                .putString(keyAccountFullName(resolvedUserId), resolvedFullName)
                .putString(keyAccountRole(resolvedUserId), resolvedRole)
                .putLong(keyAccountLastActive(resolvedUserId), now)

            avatarUrl?.let { editor.putString(keyAccountAvatarUrl(resolvedUserId), it) }

            val savedIds = getSavedAccountIds().toMutableSet()
            savedIds.add(resolvedUserId.toString())
            editor.putStringSet(KEY_SAVED_ACCOUNT_IDS, savedIds)
        }

        // commit() so a camera-startup crash cannot drop a login that only reached apply().
        editor.commit()
        if (resolvedUserId > 0L) {
            updateSessionState(resolvedUserId, resolvedRole)
        } else {
            updateSessionState(null, null)
        }
    }

    open fun getAccessToken(): String? {
        val activeId = getActiveUserId()
        if (activeId != null && activeId > 0L) {
            val encrypted = preferences.getString(keyAccountAccessToken(activeId), null)
            if (!encrypted.isNullOrBlank()) {
                val decrypted = decrypt(encrypted)
                if (!decrypted.isNullOrBlank()) return decrypted
            }
        }
        return decrypt(preferences.getString(KEY_ACCESS_TOKEN, null))
    }

    open fun getAccessTokenFor(userId: Long?): String? {
        if (userId != null && userId > 0L) {
            val encrypted = preferences.getString(keyAccountAccessToken(userId), null)
            if (!encrypted.isNullOrBlank()) {
                val decrypted = decrypt(encrypted)
                if (!decrypted.isNullOrBlank()) return decrypted
            }
            if (getActiveUserId() == userId) {
                return decrypt(preferences.getString(KEY_ACCESS_TOKEN, null))
            }
            return null
        }
        return getAccessToken()
    }

    open fun extractUserIdFromToken(token: String?): Long? {
        return extractClaimFromToken(token, "sub")?.toLongOrNull()?.takeIf { it > 0L }
    }

    open fun getRefreshToken(): String? {
        val activeId = getActiveUserId()
        if (activeId != null && activeId > 0L) {
            val encrypted = preferences.getString(keyAccountRefreshToken(activeId), null)
            if (!encrypted.isNullOrBlank()) {
                val decrypted = decrypt(encrypted)
                if (!decrypted.isNullOrBlank()) return decrypted
            }
        }
        return decrypt(preferences.getString(KEY_REFRESH_TOKEN, null))
    }

    open fun getUserId(): Long? {
        val activeId = getActiveUserId()
        if (activeId != null && activeId > 0L) {
            return activeId
        }
        return preferences.getLong(KEY_USER_ID, NO_USER_ID).takeIf { it > 0L }
    }

    open fun getUserRole(): String? {
        val activeId = getActiveUserId()
        if (activeId != null && activeId > 0L) {
            val role = preferences.getString(keyAccountRole(activeId), null)
            if (role in SUPPORTED_ROLES) return role
        }
        return preferences.getString(KEY_USER_ROLE, null)?.takeIf { it in SUPPORTED_ROLES }
    }

    open fun getActiveUserId(): Long? {
        val activeId = preferences.getLong(KEY_ACTIVE_USER_ID, NO_USER_ID)
        if (activeId > 0L) return activeId
        val legacyId = preferences.getLong(KEY_USER_ID, NO_USER_ID)
        return legacyId.takeIf { it > 0L }
    }

    open fun getActiveAccount(): SavedAccount? {
        val activeId = getActiveUserId() ?: return null
        return getSavedAccounts().firstOrNull { it.userId == activeId }
    }

    open fun getSavedAccounts(): List<SavedAccount> {
        val accountIds = getSavedAccountIds().toMutableSet()
        val activeId = getActiveUserId()
        if (activeId != null && activeId != NO_USER_ID) {
            accountIds.add(activeId.toString())
        }

        val accounts = mutableListOf<SavedAccount>()
        for (idStr in accountIds) {
            val id = idStr.toLongOrNull() ?: continue
            val email = preferences.getString(keyAccountEmail(id), null)
                ?: "user_$id@calisthenics.ai"
            val fullName = preferences.getString(keyAccountFullName(id), null)
                ?: email
            val avatarUrl = preferences.getString(keyAccountAvatarUrl(id), null)
            val role = preferences.getString(keyAccountRole(id), null) ?: "ROLE_USER"
            val lastActive = preferences.getLong(keyAccountLastActive(id), 0L)

            accounts.add(
                SavedAccount(
                    userId = id,
                    email = email,
                    fullName = fullName,
                    avatarUrl = avatarUrl,
                    role = role,
                    isActive = (id == activeId),
                    lastActiveTimestamp = lastActive
                )
            )
        }

        return accounts.sortedWith(
            compareByDescending<SavedAccount> { it.isActive }
                .thenByDescending { it.lastActiveTimestamp }
        )
    }

    open fun switchToAccount(targetUserId: Long): Boolean {
        val accountIds = getSavedAccountIds()
        val exists = accountIds.contains(targetUserId.toString()) ||
            preferences.contains(keyAccountAccessToken(targetUserId))

        if (!exists) {
            return false
        }

        val accToken = preferences.getString(keyAccountAccessToken(targetUserId), null)
        val refToken = preferences.getString(keyAccountRefreshToken(targetUserId), null)
        val role = preferences.getString(keyAccountRole(targetUserId), null)

        val editor = preferences.edit()
        editor.putLong(KEY_ACTIVE_USER_ID, targetUserId)
        editor.putLong(keyAccountLastActive(targetUserId), System.currentTimeMillis())

        if (accToken != null) editor.putString(KEY_ACCESS_TOKEN, accToken)
        if (refToken != null) editor.putString(KEY_REFRESH_TOKEN, refToken)
        editor.putLong(KEY_USER_ID, targetUserId)
        if (role in SUPPORTED_ROLES) {
            editor.putString(KEY_USER_ROLE, role)
        } else {
            editor.remove(KEY_USER_ROLE)
        }

        editor.commit()
        updateSessionState(targetUserId, role)
        return true
    }

    open fun removeAccount(userId: Long): Boolean {
        val wasActive = getActiveUserId() == userId
        val editor = preferences.edit()
        val accountIds = getSavedAccountIds().toMutableSet()
        accountIds.remove(userId.toString())
        editor.putStringSet(KEY_SAVED_ACCOUNT_IDS, accountIds)

        editor.remove(keyAccountAccessToken(userId))
        editor.remove(keyAccountRefreshToken(userId))
        editor.remove(keyAccountUserId(userId))
        editor.remove(keyAccountEmail(userId))
        editor.remove(keyAccountFullName(userId))
        editor.remove(keyAccountAvatarUrl(userId))
        editor.remove(keyAccountRole(userId))
        editor.remove(keyAccountGender(userId))
        editor.remove(keyAccountOnboardingStatus(userId))
        editor.remove(keyAccountLastActive(userId))

        if (wasActive) {
            editor.remove(KEY_ACTIVE_USER_ID)
            editor.remove(KEY_ACCESS_TOKEN)
            editor.remove(KEY_REFRESH_TOKEN)
            editor.remove(KEY_USER_ID)
            editor.remove(KEY_USER_ROLE)
        }

        editor.commit()
        if (wasActive) {
            updateSessionState(null, null)
        }
        return true
    }

    open fun updateAccountProfile(
        userId: Long,
        email: String? = null,
        fullName: String? = null,
        avatarUrl: String? = null
    ) {
        val editor = preferences.edit()
        email?.takeIf { it.isNotBlank() }?.let { editor.putString(keyAccountEmail(userId), it) }
        fullName?.takeIf { it.isNotBlank() }?.let { editor.putString(keyAccountFullName(userId), it) }
        avatarUrl?.let { editor.putString(keyAccountAvatarUrl(userId), it) }
        editor.apply()
    }

    open fun saveAccountGender(userId: Long, gender: String?) {
        if (userId <= 0L) return
        val editor = preferences.edit()
        val normalized = gender?.trim()?.uppercase()?.takeIf { it == "MALE" || it == "FEMALE" }
        if (normalized != null) {
            editor.putString(keyAccountGender(userId), normalized)
        } else {
            editor.remove(keyAccountGender(userId))
        }
        editor.apply()
    }

    open fun getAccountGender(userId: Long): String? {
        if (userId <= 0L) return null
        return preferences.getString(keyAccountGender(userId), null)
    }

    open fun saveAccountOnboardingStatus(userId: Long, status: String?) {
        if (userId <= 0L) return
        val editor = preferences.edit()
        val normalized = status?.trim()?.uppercase()?.takeIf {
            it == "NOT_SHOWN" || it == "COMPACT_BANNER" || it == "DISMISSED"
        }
        if (normalized != null) {
            editor.putString(keyAccountOnboardingStatus(userId), normalized)
        } else {
            editor.remove(keyAccountOnboardingStatus(userId))
        }
        editor.apply()
    }

    open fun getAccountOnboardingStatus(userId: Long): String? {
        if (userId <= 0L) return null
        return preferences.getString(keyAccountOnboardingStatus(userId), null)
    }

    open fun deactivateActiveSession() {
        val hadActiveUser = _sessionState.value.activeUserId != null || getActiveUserId() != null
        preferences.edit()
            .remove(KEY_ACTIVE_USER_ID)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_ROLE)
            .commit()
        updateSessionState(null, null, forceBumpGeneration = hadActiveUser)
    }

    open fun getUserEmail(): String? {
        val activeId = getActiveUserId()
        if (activeId != null && activeId > 0L) {
            return preferences.getString(keyAccountEmail(activeId), null)
        }
        return null
    }

    open fun clear() {
        val activeId = getActiveUserId()
        if (activeId != null && activeId != NO_USER_ID) {
            removeAccount(activeId)
        } else {
            val hadSession = _sessionState.value.activeUserId != null
            preferences.edit()
                .remove(KEY_ACTIVE_USER_ID)
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_ROLE)
                .commit()
            updateSessionState(null, null, forceBumpGeneration = hadSession)
        }
    }

    open fun clearAll() {
        val hadSession = _sessionState.value.activeUserId != null || getActiveUserId() != null
        preferences.edit().clear().commit()
        updateSessionState(null, null, forceBumpGeneration = hadSession)
    }

    open fun hasValidSession(): Boolean {
        val token = getAccessToken()
        return !token.isNullOrBlank() && !isRefreshTokenExpired()
    }

    open fun isAccessTokenExpired(): Boolean {
        return isTokenExpired(getAccessToken())
    }

    open fun isRefreshTokenExpired(): Boolean {
        return isTokenExpired(getRefreshToken())
    }

    open fun isTokenExpired(token: String?): Boolean {
        if (token.isNullOrBlank()) return true
        return try {
            val parts = token.split('.')
            if (parts.size < 2) return true
            val payloadBytes = decodeBase64UrlSafe(parts[1])
            val payloadString = String(payloadBytes, StandardCharsets.UTF_8)
            val jsonObject = JsonParser.parseString(payloadString).asJsonObject
            if (!jsonObject.has("exp")) return false
            val expSeconds = jsonObject.get("exp").asLong
            val currentSeconds = System.currentTimeMillis() / 1000
            currentSeconds >= expSeconds
        } catch (_: Throwable) {
            true
        }
    }

    private fun extractClaimFromToken(token: String?, claimKey: String): String? {
        if (token.isNullOrBlank()) return null
        return try {
            val parts = token.split('.')
            if (parts.size < 2) return null
            val payloadBytes = decodeBase64UrlSafe(parts[1])
            val payloadString = String(payloadBytes, StandardCharsets.UTF_8)
            val jsonObject = JsonParser.parseString(payloadString).asJsonObject
            if (!jsonObject.has(claimKey)) return null
            jsonObject.get(claimKey).asString
        } catch (_: Throwable) {
            null
        }
    }

    private fun extractRoleFromToken(token: String?): String? {
        if (token.isNullOrBlank()) return null
        return try {
            val parts = token.split('.')
            if (parts.size < 2) return null
            val payloadBytes = decodeBase64UrlSafe(parts[1])
            val payloadString = String(payloadBytes, StandardCharsets.UTF_8)
            val jsonObject = JsonParser.parseString(payloadString).asJsonObject
            when {
                jsonObject.has("role") -> jsonObject.get("role").asString
                jsonObject.has("roles") -> {
                    val rolesElement = jsonObject.get("roles")
                    if (rolesElement.isJsonArray) {
                        val array = rolesElement.asJsonArray
                        if (array.size() > 0) array.get(0).asString else null
                    } else {
                        rolesElement.asString
                    }
                }
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun getSavedAccountIds(): Set<String> {
        val raw = preferences.getStringSet(KEY_SAVED_ACCOUNT_IDS, null)
        return raw?.toSet() ?: emptySet()
    }

    private fun keyAccountAccessToken(id: Long) = "account_${id}_access_token"
    private fun keyAccountRefreshToken(id: Long) = "account_${id}_refresh_token"
    private fun keyAccountUserId(id: Long) = "account_${id}_user_id"
    private fun keyAccountEmail(id: Long) = "account_${id}_email"
    private fun keyAccountFullName(id: Long) = "account_${id}_full_name"
    private fun keyAccountAvatarUrl(id: Long) = "account_${id}_avatar_url"
    private fun keyAccountRole(id: Long) = "account_${id}_role"
    private fun keyAccountGender(id: Long) = "account_${id}_gender"
    private fun keyAccountOnboardingStatus(id: Long) = "account_${id}_onboarding_status"
    private fun keyAccountLastActive(id: Long) = "account_${id}_last_active"

    private fun decodeBase64UrlSafe(encoded: String): ByteArray {
        return try {
            java.util.Base64.getUrlDecoder().decode(encoded)
        } catch (_: Throwable) {
            Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        }
    }

    private fun decodeBase64(encoded: String): ByteArray {
        return try {
            Base64.decode(encoded, Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getDecoder().decode(encoded)
        }
    }

    private fun encodeBase64(bytes: ByteArray): String {
        return try {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    private fun encryptionKey(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }
        val generator = KeyGenerator.getInstance(KEY_ALGORITHM, ANDROID_KEYSTORE)
        generator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(value: String): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
            val iv = encodeBase64(cipher.iv)
            val payload = encodeBase64(cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8)))
            "$iv.$payload"
        } catch (_: Throwable) {
            // Fallback for JVM test environments without AndroidKeyStore
            "enc.$value"
        }
    }

    private fun decrypt(encoded: String?): String? {
        if (encoded.isNullOrBlank()) return null
        if (encoded.startsWith("enc.")) return encoded.removePrefix("enc.")
        return runCatching {
            val parts = encoded.split('.', limit = 2)
            if (parts.size != 2) return null

            val iv = decodeBase64(parts[0])
            val payload = decodeBase64(parts[1])
            if (iv.isEmpty() || payload.isEmpty()) return null

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(payload), StandardCharsets.UTF_8)
        }.getOrNull()
    }

    companion object {
        private const val PREF_NAME = "calisthenics_auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_ACTIVE_USER_ID = "active_user_id"
        private const val KEY_SAVED_ACCOUNT_IDS = "saved_account_ids"
        private const val NO_USER_ID = -1L
        private val SUPPORTED_ROLES = setOf("ROLE_USER", "ROLE_ADMIN")
        private const val KEY_ALIAS = "calisthenics_auth_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
