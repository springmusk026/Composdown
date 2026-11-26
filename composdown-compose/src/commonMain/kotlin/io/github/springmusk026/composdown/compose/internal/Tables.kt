package io.github.springmusk026.composdown.compose.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.TableAlignment
import io.github.springmusk026.composdown.core.ast.TableCell

/** Cells wrap their text beyond this width instead of widening the column further. */
private val MaxColumnWidth = 280.dp

private val CellBorderWidth = 1.dp

/**
 * A table whose columns are as wide as their widest cell (up to [MaxColumnWidth]), scrolling
 * horizontally when wider than the available space. Exposes row and column structure to
 * accessibility services.
 */
@Composable
internal fun DefaultTable(
    block: Block.Table,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(block.header) + block.rows
    val columns = block.columns.size
    Box(modifier = modifier.horizontalScroll(rememberScrollState())) {
        Layout(
            modifier =
                Modifier.semantics {
                    collectionInfo =
                        CollectionInfo(
                            rowCount = rows.size,
                            columnCount = columns,
                        )
                },
            measurePolicy = tableMeasurePolicy(rows.size, columns, MaxColumnWidth),
            content = {
                rows.forEachIndexed { rowIndex, row ->
                    row.cells.forEachIndexed { columnIndex, cell ->
                        TableCellContent(cell, rowIndex, columnIndex, block.columns[columnIndex])
                    }
                }
            },
        )
    }
}

@Composable
private fun TableCellContent(
    cell: TableCell,
    rowIndex: Int,
    columnIndex: Int,
    alignment: TableAlignment,
    modifier: Modifier = Modifier,
) {
    val theme = currentEnvironment().theme
    val isHeader = rowIndex == 0
    Box(
        modifier =
            modifier
                .border(CellBorderWidth, theme.colors.divider)
                .background(if (isHeader) theme.colors.tableHeaderBackground else Color.Transparent)
                .padding(theme.spacing.tableCellPadding)
                .semantics { collectionItemInfo = CollectionItemInfo(rowIndex, 1, columnIndex, 1) },
    ) {
        val style = if (isHeader) theme.typography.tableHeader else theme.typography.tableCell
        InlineContent(cell.content, style.merge(textAlign = alignment.textAlign()))
    }
}

/**
 * Lays out cells (given row by row) in a grid. Columns are as wide as their widest cell, up to
 * [maxColumnWidth]; rows are as tall as their tallest cell. Sizes come from intrinsic measurements,
 * so that each cell is measured only once, at its final size, and cell borders line up.
 */
private fun tableMeasurePolicy(
    rows: Int,
    columns: Int,
    maxColumnWidth: Dp,
) = MeasurePolicy { measurables, constraints ->
    val maxWidth = maxColumnWidth.roundToPx()
    val widths =
        IntArray(columns) { column ->
            (0 until rows)
                .maxOf { row -> measurables[row * columns + column].maxIntrinsicWidth(Constraints.Infinity) }
                .coerceAtMost(maxWidth)
        }
    val heights =
        IntArray(rows) { row ->
            (0 until columns).maxOf { column -> measurables[row * columns + column].minIntrinsicHeight(widths[column]) }
        }
    val placeables =
        measurables.mapIndexed { index, measurable ->
            measurable.measure(Constraints.fixed(widths[index % columns], heights[index / columns]))
        }
    layout(widths.sum().coerceAtLeast(constraints.minWidth), heights.sum()) {
        var y = 0
        for (row in 0 until rows) {
            var x = 0
            for (column in 0 until columns) {
                placeables[row * columns + column].placeRelative(x, y)
                x += widths[column]
            }
            y += heights[row]
        }
    }
}

private fun TableAlignment.textAlign(): TextAlign =
    when (this) {
        TableAlignment.NONE, TableAlignment.LEFT -> TextAlign.Start
        TableAlignment.CENTER -> TextAlign.Center
        TableAlignment.RIGHT -> TextAlign.End
    }
