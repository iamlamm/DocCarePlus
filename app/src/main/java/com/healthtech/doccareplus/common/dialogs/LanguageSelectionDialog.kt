package com.healthtech.doccareplus.common.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.DialogLanguageSelectionBinding
import com.healthtech.doccareplus.utils.Constants

class LanguageSelectionDialog(
    context: Context,
    private val currentLanguage: String,
    private val onLanguageSelected: (String) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogLanguageSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogLanguageSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set current selection
        when (currentLanguage) {
            Constants.LANGUAGE_ENGLISH -> binding.radioEnglish.isChecked = true
            Constants.LANGUAGE_VIETNAMESE -> binding.radioVietnamese.isChecked = true
        }

        binding.languageRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedLanguage = when (checkedId) {
                R.id.radio_english -> Constants.LANGUAGE_ENGLISH
                R.id.radio_vietnamese -> Constants.LANGUAGE_VIETNAMESE
                else -> Constants.DEFAULT_LANGUAGE
            }
            onLanguageSelected(selectedLanguage)
            dismiss()
        }
    }
}