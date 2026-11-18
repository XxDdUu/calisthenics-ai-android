package com.calisthenics.ai.ui.components.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun test1_multipleHeadings() {
        val markdown = """
            # Heading 1
            ## Heading 2
            ### Heading 3
            #### Heading 4
            ##### Heading 5
            ###### Heading 6
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(markdown)
        assertEquals(6, blocks.size)

        for (i in 1..6) {
            val heading = blocks[i - 1] as MarkdownBlock.Heading
            assertEquals(i, heading.level)
            assertEquals("Heading $i", heading.text)
        }
    }

    @Test
    fun test2_boldItalicStrikethroughInlineCode() {
        val line = "Here is **bold**, *italic*, ***bolditalic***, ~~striked~~, and `val x = 10`."
        val spans = MarkdownParser.parseInline(line)

        // Verify span types
        assertTrue(spans.any { it is MarkdownSpan.Bold })
        assertTrue(spans.any { it is MarkdownSpan.Italic })
        assertTrue(spans.any { it is MarkdownSpan.BoldItalic })
        assertTrue(spans.any { it is MarkdownSpan.Strikethrough })
        assertTrue(spans.any { it is MarkdownSpan.InlineCode })

        val codeSpan = spans.filterIsInstance<MarkdownSpan.InlineCode>().first()
        assertEquals("val x = 10", codeSpan.code)

        val strikeSpan = spans.filterIsInstance<MarkdownSpan.Strikethrough>().first()
        val strikeInner = strikeSpan.content.filterIsInstance<MarkdownSpan.Text>().first()
        assertEquals("striked", strikeInner.content)
    }

    @Test
    fun test3_nestedBulletListsAndChecklists() {
        val markdown = """
            - Parent item 1
              - Nested child 1
                - Grandchild 1
            - [ ] Incomplete task
            - [x] Completed task
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(markdown)
        assertEquals(1, blocks.size)

        val listGroup = blocks[0] as MarkdownBlock.ListGroup
        assertEquals(5, listGroup.items.size)

        assertEquals(0, listGroup.items[0].depth)
        assertFalse(listGroup.items[0].isOrdered)
        assertNull(listGroup.items[0].isChecked)
        assertEquals("Parent item 1", listGroup.items[0].text)

        assertEquals(1, listGroup.items[1].depth)
        assertEquals("Nested child 1", listGroup.items[1].text)

        assertEquals(2, listGroup.items[2].depth)
        assertEquals("Grandchild 1", listGroup.items[2].text)

        assertEquals(0, listGroup.items[3].depth)
        assertEquals(false, listGroup.items[3].isChecked)
        assertEquals("Incomplete task", listGroup.items[3].text)

        assertEquals(0, listGroup.items[4].depth)
        assertEquals(true, listGroup.items[4].isChecked)
        assertEquals("Completed task", listGroup.items[4].text)
    }

    @Test
    fun test4_numberedInstructions() {
        val markdown = """
            1. Warm up with arm swings for 3 minutes
            2. Perform 3 sets of Planche leans
            3. Cool down and stretch wrists
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(markdown)
        assertEquals(1, blocks.size)

        val listGroup = blocks[0] as MarkdownBlock.ListGroup
        assertEquals(3, listGroup.items.size)

        for (i in 1..3) {
            val item = listGroup.items[i - 1]
            assertTrue(item.isOrdered)
            assertEquals(i, item.number)
            assertEquals(0, item.depth)
        }
        assertEquals("Warm up with arm swings for 3 minutes", listGroup.items[0].text)
    }

    @Test
    fun test5_tablesWithAlignments() {
        val markdown = """
            | Exercise | Sets | Reps / Duration | Difficulty |
            | :--- | :---: | ---: | --- |
            | Tuck Planche | 4 | 15s hold | Advanced |
            | Push-ups | 3 | 12 reps | Beginner |
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(markdown)
        assertEquals(1, blocks.size)

        val table = blocks[0] as MarkdownBlock.Table
        assertEquals(4, table.headers.size)
        assertEquals("Exercise", table.headers[0].rawText)
        assertEquals("Sets", table.headers[1].rawText)
        assertEquals("Reps / Duration", table.headers[2].rawText)
        assertEquals("Difficulty", table.headers[3].rawText)

        assertEquals(TableAlignment.LEFT, table.alignments[0])
        assertEquals(TableAlignment.CENTER, table.alignments[1])
        assertEquals(TableAlignment.RIGHT, table.alignments[2])
        assertEquals(TableAlignment.LEFT, table.alignments[3])

        assertEquals(2, table.rows.size)
        assertEquals("Tuck Planche", table.rows[0][0].rawText)
        assertEquals("4", table.rows[0][1].rawText)
        assertEquals("15s hold", table.rows[0][2].rawText)
        assertEquals("Advanced", table.rows[0][3].rawText)

        assertEquals("Push-ups", table.rows[1][0].rawText)
    }

    @Test
    fun test6_codeBlocksWithLanguageTags() {
        val markdown = """
            ```kotlin
            fun trainPlanche(user: User): TrainingResult {
                return TrainingResult(completed = true)
            }
            ```
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(markdown)
        assertEquals(1, blocks.size)

        val codeBlock = blocks[0] as MarkdownBlock.CodeBlock
        assertEquals("kotlin", codeBlock.language)
        assertTrue(codeBlock.code.contains("fun trainPlanche(user: User)"))
    }

    @Test
    fun test7_linksAndAutolinks() {
        val textWithLink = "Visit [SKY](https://sky-enterprise.id.vn) or https://google.com for info."
        val spans = MarkdownParser.parseInline(textWithLink)

        val links = spans.filterIsInstance<MarkdownSpan.Link>()
        assertEquals(2, links.size)

        assertEquals("SKY", links[0].text)
        assertEquals("https://sky-enterprise.id.vn", links[0].url)

        assertEquals("https://google.com", links[1].text)
        assertEquals("https://google.com", links[1].url)
    }

    @Test
    fun test8_mixedVietnameseAndEnglishDiacritics() {
        val markdown = """
            ### Lộ trình tập luyện Planche & Hít đất
            - **Khởi động**: Xoay cổ tay 2 phút, kéo giãn vai.
            - **Bài tập chính**: *Tuck Planche Lean* giữ vững 15 giây.
            - Chú ý: Lưng thẳng, siết cơ bụng và không sụp vai!
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(markdown)
        assertEquals(2, blocks.size)

        val heading = blocks[0] as MarkdownBlock.Heading
        assertEquals(3, heading.level)
        assertEquals("Lộ trình tập luyện Planche & Hít đất", heading.text)

        val list = blocks[1] as MarkdownBlock.ListGroup
        assertEquals(3, list.items.size)
        assertTrue(list.items[0].text.contains("Khởi động"))
        assertTrue(list.items[1].text.contains("Tuck Planche Lean"))
        assertTrue(list.items[2].text.contains("Lưng thẳng, siết cơ bụng"))
    }

    @Test
    fun test9_longComplexResponse() {
        val complexMarkdown = """
            # Giáo án luyện tập tuần 1

            Chào mừng bạn đến với lộ trình Calisthenics nâng cao!

            ---

            ## Mục tiêu chính
            > "Kỷ luật tạo nên sức mạnh bền bỉ."

            1. Nâng thời gian giữ Tuck Planche lên 20s.
            2. Cải thiện độ mở vai trong bài Handstand.

            ### Danh sách bài tập
            | Bài tập | Hiệp | Thời gian / Số lần |
            | :--- | :---: | ---: |
            | Pseudo Planche Push-ups | 4 | 8-10 reps |
            | Tuck Planche Hold | 5 | 15s hold |
            | Pike Push-ups | 3 | 10 reps |

            ### Đoạn mã cấu hình bài tập
            ```json
            {
              "program": "Planche Mastery",
              "phase": 1,
              "difficulty": "INTERMEDIATE"
            }
            ```

            - [x] Hoàn thành bài tập khởi động
            - [ ] Hoàn thành bài tập giữ thăng bằng
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(complexMarkdown)
        // Should parse into:
        // 1. Heading (H1)
        // 2. Paragraph
        // 3. HorizontalRule
        // 4. Heading (H2)
        // 5. Blockquote
        // 6. ListGroup (ordered 1, 2)
        // 7. Heading (H3)
        // 8. Table
        // 9. Heading (H3)
        // 10. CodeBlock
        // 11. ListGroup (checklists)
        assertTrue(blocks.size >= 10)

        assertTrue(blocks.any { it is MarkdownBlock.Heading && it.level == 1 })
        assertTrue(blocks.any { it is MarkdownBlock.HorizontalRule })
        assertTrue(blocks.any { it is MarkdownBlock.Blockquote })
        assertTrue(blocks.any { it is MarkdownBlock.Table })
        assertTrue(blocks.any { it is MarkdownBlock.CodeBlock })
        assertTrue(blocks.any { it is MarkdownBlock.ListGroup })
    }

    @Test
    fun test10_streamingAndUnclosedBlocks() {
        // 1. Unclosed code block
        val streamingCode = "```kotlin\nval temp = 42"
        val codeBlocks = MarkdownParser.parseBlocks(streamingCode)
        assertEquals(1, codeBlocks.size)
        val codeBlock = codeBlocks[0] as MarkdownBlock.CodeBlock
        assertEquals("kotlin", codeBlock.language)
        assertEquals("val temp = 42", codeBlock.code)

        // 2. Unclosed inline formatting (bold, inline code, link)
        val streamingInline = "This is **partially typed bold and `incomplete code"
        val spans = MarkdownParser.parseInline(streamingInline)
        // Must not crash or throw exceptions, and must preserve the text content
        val combinedText = spans.joinToString("") {
            when (it) {
                is MarkdownSpan.Text -> it.content
                is MarkdownSpan.Bold -> it.content.joinToString("") { inner -> (inner as? MarkdownSpan.Text)?.content ?: "" }
                is MarkdownSpan.InlineCode -> it.code
                else -> ""
            }
        }
        assertTrue(combinedText.contains("partially typed bold"))

        // 3. Unclosed table row
        val partialTable = """
            | Skill | Level |
            | :--- | :--- |
            | Front Lever | Int
        """.trimIndent()
        val tableBlocks = MarkdownParser.parseBlocks(partialTable)
        assertEquals(1, tableBlocks.size)
        val table = tableBlocks[0] as MarkdownBlock.Table
        assertEquals(2, table.headers.size)
        assertEquals(1, table.rows.size)
        assertEquals("Front Lever", table.rows[0][0].rawText)
        assertEquals("Int", table.rows[0][1].rawText)
    }
}
