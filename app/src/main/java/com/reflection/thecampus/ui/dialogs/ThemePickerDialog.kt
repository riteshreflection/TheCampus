package com.reflection.thecampus.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.reflection.thecampus.R
import com.reflection.thecampus.utils.ThemeManager

class ThemePickerDialog : BottomSheetDialogFragment() {

    private var onThemeSelected: ((ThemeManager.ColorTheme) -> Unit)? = null
    private var selectedTheme: ThemeManager.ColorTheme? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_theme_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentTheme = ThemeManager.getCurrentTheme(requireContext())
        selectedTheme = currentTheme

        val rvColorThemes = view.findViewById<RecyclerView>(R.id.rvColorThemes)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnApply = view.findViewById<MaterialButton>(R.id.btnApply)

        // Setup RecyclerView
        rvColorThemes.layoutManager = LinearLayoutManager(requireContext())
        val adapter = ThemeAdapter(
            themes = ThemeManager.getAllThemes(),
            currentTheme = currentTheme,
            isDarkMode = isDarkMode()
        ) { theme ->
            selectedTheme = theme
        }
        rvColorThemes.adapter = adapter

        // Cancel button
        btnCancel.setOnClickListener {
            dismiss()
        }

        // Apply button
        btnApply.setOnClickListener {
            selectedTheme?.let { theme ->
                onThemeSelected?.invoke(theme)
                dismiss()
            }
        }
    }

    private fun isDarkMode(): Boolean {
        val nightModeFlags = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    fun setOnThemeSelectedListener(listener: (ThemeManager.ColorTheme) -> Unit) {
        onThemeSelected = listener
    }

    companion object {
        const val TAG = "ThemePickerDialog"

        fun newInstance(): ThemePickerDialog {
            return ThemePickerDialog()
        }
    }
}

/**
 * Adapter for theme selection RecyclerView
 */
class ThemeAdapter(
    private val themes: List<ThemeManager.ColorTheme>,
    private var currentTheme: ThemeManager.ColorTheme,
    private val isDarkMode: Boolean,
    private val onThemeClick: (ThemeManager.ColorTheme) -> Unit
) : RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder>() {

    private var selectedPosition = themes.indexOf(currentTheme)

    inner class ThemeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val colorPreview: View = view.findViewById(R.id.viewColorPreview)
        val themeName: android.widget.TextView = view.findViewById(R.id.tvThemeName)
        val selectedIcon: android.widget.ImageView = view.findViewById(R.id.ivSelected)
        val currentBadge: android.widget.TextView = view.findViewById(R.id.tvCurrentBadge)
        val card: com.google.android.material.card.MaterialCardView = view.findViewById(R.id.cardTheme)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color_theme, parent, false)
        return ThemeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        val theme = themes[position]
        val color = if (isDarkMode) theme.darkPrimary else theme.lightPrimary

        holder.themeName.text = theme.themeName
        holder.colorPreview.setBackgroundColor(color)

        // Show selection indicator
        if (position == selectedPosition) {
            holder.selectedIcon.visibility = View.VISIBLE
            holder.card.strokeWidth = 4
            holder.card.strokeColor = color
        } else {
            holder.selectedIcon.visibility = View.GONE
            holder.card.strokeWidth = 0
        }

        // Show current badge
        holder.currentBadge.visibility = if (theme == currentTheme) View.VISIBLE else View.GONE

        // Click listener
        holder.card.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            onThemeClick(theme)
        }
    }

    override fun getItemCount() = themes.size
}
