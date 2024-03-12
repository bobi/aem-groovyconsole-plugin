package com.github.bobi.aemgroovyconsoleplugin.execution.table

import com.intellij.application.options.EditorFontsConstants
import com.intellij.ide.ui.UISettings
import com.intellij.ide.ui.UISettingsUtils
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.util.ObjectUtils
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Font
import kotlin.math.max
import kotlin.math.min

internal class AemConsoleTableColorScheme {
    private val fontSizeIncrement = 0.0

    private val fontSizeScale = 1.0

    private val colorsScheme: EditorColorsScheme
        get() = EditorColorsManager.getInstance().schemeForCurrentUITheme

    internal val lineSpacing: Float
        get() = colorsScheme.lineSpacing

    internal val backgroundColor: Color
        get() = ObjectUtils.chooseNotNull(colorsScheme.defaultBackground, UIUtil.getTableBackground())

    internal val cellFocusingBackground: Color
        get() = ObjectUtils.chooseNotNull(
            colorsScheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR), UIUtil.getTableSelectionBackground(true)
        )

    internal val cellFocusingForeground: Color
        get() = ObjectUtils.chooseNotNull(
            colorsScheme.getColor(EditorColors.SELECTION_FOREGROUND_COLOR), UIUtil.getTableSelectionForeground(true)
        )

    internal val cellSearchBackground: Color
        get() = ObjectUtils.chooseNotNull(
            colorsScheme.getAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES)?.backgroundColor, cellFocusingBackground
        )

    internal val selectionBackground: Color
        get() = UIUtil.getTableSelectionBackground(false)

    internal val selectionForeground: Color
        get() = UIUtil.getTableSelectionForeground(false)

    internal val gridColor: Color
        get() = ObjectUtils.chooseNotNull(
            colorsScheme.getColor(EditorColors.INDENT_GUIDE_COLOR), UIUtil.getTableGridColor()
        )

    internal val labelDisabledForegroundColor: Color
        get() = UIUtil.getLabelDisabledForeground()

    internal val defaultCellEmptyBorder = JBUI.Borders.compound(JBUI.Borders.empty(1, 2), JBUI.Borders.empty(1))

    private fun getFont(): Font = colorsScheme.getFont(EditorFontType.PLAIN)

    internal fun getScaledFont(): Font {
        val font = getFont()

        return if (UISettings.getInstance().presentationMode) {
            font
        } else {
            font.deriveFont(fontSize())
        }
    }

    private fun fontSize(): Int {
        val baseFontSize = Math.round(UISettingsUtils.getInstance().scaleFontSize(colorsScheme.editorFontSize2D))
        val newFontSize = max(fontSizeScale * baseFontSize + fontSizeIncrement, 8.0).toInt()

        return min(
            max(EditorFontsConstants.getMaxEditorFontSize().toDouble(), baseFontSize.toDouble()), newFontSize.toDouble()
        ).toInt()
    }
}
