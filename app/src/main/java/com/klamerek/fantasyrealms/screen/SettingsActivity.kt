package com.klamerek.fantasyrealms.screen

import android.os.Bundle
import android.widget.ArrayAdapter
import com.klamerek.fantasyrealms.R
import com.klamerek.fantasyrealms.databinding.ActivitySettingsBinding
import com.klamerek.fantasyrealms.game.CardDefinitions
import com.klamerek.fantasyrealms.game.DiscardArea
import com.klamerek.fantasyrealms.game.Player
import com.klamerek.fantasyrealms.toInt
import com.klamerek.fantasyrealms.util.Language
import com.klamerek.fantasyrealms.util.LocaleManager
import com.klamerek.fantasyrealms.util.LocaleManager.getLanguage
import com.klamerek.fantasyrealms.util.LocaleManager.languages
import com.klamerek.fantasyrealms.util.Preferences


/**
 * Activity for managing application settings.
 */
class SettingsActivity : CustomActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            displayCardNumberCheckBox.isChecked = Preferences.getDisplayCardNumber(baseContext)
            removeAlreadySelectedCheckBox.isChecked = Preferences.getRemoveAlreadySelected(baseContext)
            withBuildingsOutsidersUndeadCheckBox.isChecked =
                Preferences.getBuildingsOutsidersUndead(baseContext)
            withCursedItemsCheckBox.isChecked = Preferences.getCursedItems(baseContext)
            displayChipColorOnSearchCheckBox.isChecked =
                Preferences.getDisplayChipColorOnSearch(baseContext)
            deluxeEditionCheckBox.isChecked = Preferences.getDeluxeEdition(baseContext)
            ocrMatchingScoreEditText.setText(
                Preferences.getMatchingCardScoreThreshold(baseContext).toString()
            )
            ocrDifferenceLengthEditText.setText(
                Preferences.getDifferenceLengthInNameThreshold(baseContext).toString()
            )
        }

        val initialValue = getCardScopeId()

        val languageAdapter = ArrayAdapter(this, R.layout.custom_spinner_list_item, languages)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.languageSpinner.adapter = languageAdapter

        val scanModeAdapter =
            ArrayAdapter(this, R.layout.custom_spinner_list_item, Preferences.scanModes)
        scanModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.scanModeSpinner.adapter = scanModeAdapter

        binding.doneButton.setOnClickListener {
            val language = binding.languageSpinner.selectedItem as Language
            LocaleManager.saveLanguageInPreferences(baseContext, language)

            Preferences.apply {
                saveScanModeInPreferences(
                    baseContext,
                    binding.scanModeSpinner.selectedItem.toString()
                )
                saveDisplayCardNumberInPreferences(
                    baseContext,
                    binding.displayCardNumberCheckBox.isChecked
                )
                saveBuildingsOutsidersUndeadInPreferences(
                    baseContext,
                    binding.withBuildingsOutsidersUndeadCheckBox.isChecked
                )
                saveCursedItemsInPreferences(
                    baseContext,
                    binding.withCursedItemsCheckBox.isChecked
                )
                saveDisplayChipColorOnSearchInPreferences(
                    baseContext,
                    binding.displayChipColorOnSearchCheckBox.isChecked
                )
                saveRemoveAlreadySelectedInPreferences(
                    baseContext,
                    binding.removeAlreadySelectedCheckBox.isChecked
                )
                saveDeluxeEditionInPreferences(
                    baseContext,
                    binding.deluxeEditionCheckBox.isChecked
                )
                saveMatchingCardScoreThresholdInPreferences(
                    baseContext,
                    binding.ocrMatchingScoreEditText.text.toString().toIntOrNull() ?: 90
                )
                saveDifferenceLengthInNameThresholdInPreferences(
                    baseContext,
                    binding.ocrDifferenceLengthEditText.text.toString().toIntOrNull() ?: 0
                )
            }
            removeCardOutOfScope(initialValue)
            finishAfterTransition()
        }

        binding.languageSpinner.setSelection(
            (binding.languageSpinner.adapter as ArrayAdapter<Language>).getPosition(
                getLanguage(this)
            )
        )

        binding.scanModeSpinner.setSelection(
            (binding.scanModeSpinner.adapter as ArrayAdapter<String>).getPosition(
                Preferences.getScanMode(baseContext)
            )
        )

    }

    /**
     * If card scope changed, we remove obsolete cards
     */
    private fun removeCardOutOfScope(initialValue: String) {
        if (initialValue != getCardScopeId()) {
            val scope = CardDefinitions.get(baseContext)
            Player.all.plus(DiscardArea.instance).forEach { withGame ->
                withGame.game().cards().map { it.definition }
                    .filter { !scope.contains(it) }.toList()
                    .forEach { cardToRemove -> withGame.game().remove(cardToRemove) }
            }
        }
    }

    private fun getCardScopeId() =
        binding.withBuildingsOutsidersUndeadCheckBox.isChecked.toInt().toString() +
                binding.withCursedItemsCheckBox.isChecked.toInt().toString() +
                binding.deluxeEditionCheckBox.isChecked.toInt().toString()

}
