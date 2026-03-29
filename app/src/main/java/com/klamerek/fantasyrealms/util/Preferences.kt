package com.klamerek.fantasyrealms.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.klamerek.fantasyrealms.R

object Preferences {

    const val SCAN_MODE_DEFAULT = "Default"
    private const val SCAN_MODE_ON_THE_FLY = "On the fly"
    val scanModes = listOf(SCAN_MODE_DEFAULT, SCAN_MODE_ON_THE_FLY)

    fun sharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(
            context.getString(R.string.preference_file_key), Context.MODE_PRIVATE
        )
    }

    fun getRemoveAlreadySelected(context: Context): Boolean {
        return sharedPreferences(context).getBoolean(
            context.getString(R.string.remove_already_selected),
            true
        )
    }

    fun saveRemoveAlreadySelectedInPreferences(context: Context, accept: Boolean) {
        sharedPreferences(context).edit {
            putBoolean(context.getString(R.string.remove_already_selected), accept)
        }
    }

    fun getDisplayCardNumber(context: Context): Boolean {
        return sharedPreferences(context).getBoolean(
            context.getString(R.string.display_card_number),
            false
        )
    }

    fun saveDisplayCardNumberInPreferences(context: Context, display: Boolean) {
        sharedPreferences(context).edit {
            putBoolean(context.getString(R.string.display_card_number), display)
        }
    }

    fun getCursedItems(context: Context): Boolean {
        return sharedPreferences(context).getBoolean(
            context.getString(R.string.cursed_items),
            false
        )
    }

    fun saveCursedItemsInPreferences(context: Context, activate: Boolean) {
        sharedPreferences(context).edit {
            putBoolean(context.getString(R.string.cursed_items), activate)
        }
    }

    fun getBuildingsOutsidersUndead(context: Context): Boolean {
        return sharedPreferences(context).getBoolean(
            context.getString(R.string.buildings_outsiders_undead),
            false
        )
    }

    fun saveBuildingsOutsidersUndeadInPreferences(context: Context, activate: Boolean) {
        sharedPreferences(context).edit {
            putBoolean(context.getString(R.string.buildings_outsiders_undead), activate)
        }
    }

    fun getWildfireWithOutsiders(context: Context): Boolean {
        return sharedPreferences(context).getBoolean(
            context.getString(R.string.wildfire_with_outsiders),
            false
        )
    }

    fun saveWildfireWithOutsidersInPreferences(context: Context, activate: Boolean) {
        sharedPreferences(context).edit {
            putBoolean(context.getString(R.string.wildfire_with_outsiders), activate)
        }
    }

    fun getPhoenixDeluxeEdition(context: Context): Boolean {
        return sharedPreferences(context).getBoolean(
            context.getString(R.string.phoenix_deluxe_edition),
            false
        )
    }

    fun savePhoenixDeluxeEditionInPreferences(context: Context, activate: Boolean) {
        sharedPreferences(context).edit {
            putBoolean(context.getString(R.string.phoenix_deluxe_edition), activate)
        }
    }

    fun saveDisplayChipColorOnSearchInPreferences(context: Context, activate: Boolean) {
        sharedPreferences(context).edit {
            putBoolean(context.getString(R.string.display_chip_color_on_search), activate)
        }
    }

    fun getDisplayChipColorOnSearch(context: Context): Boolean {
        return sharedPreferences(context).getBoolean(
            context.getString(R.string.display_chip_color_on_search),
            true
        )
    }

    fun saveScanModeInPreferences(context: Context, mode: String) {
        sharedPreferences(context).edit {
            putString(context.getString(R.string.scan_mode), mode)
        }
    }

    fun getScanMode(context: Context): String {
        return sharedPreferences(context).getString(
            context.getString(R.string.scan_mode),
            SCAN_MODE_DEFAULT
        )!!
    }

    fun getMatchingCardScoreThreshold(context: Context): Int {
        return sharedPreferences(context).getInt(
            context.getString(R.string.ocr_matching_score_threshold),
            Constants.MATCHING_CARD_SCORE_THRESHOLD
        )
    }

    fun saveMatchingCardScoreThresholdInPreferences(context: Context, threshold: Int) {
        sharedPreferences(context).edit {
            putInt(context.getString(R.string.ocr_matching_score_threshold), threshold)
        }
    }

    fun getDifferenceLengthInNameThreshold(context: Context): Int {
        return sharedPreferences(context).getInt(
            context.getString(R.string.ocr_difference_length_threshold),
            Constants.DIFFERENCE_LENGTH_IN_NAME_THRESHOLD
        )
    }

    fun saveDifferenceLengthInNameThresholdInPreferences(context: Context, threshold: Int) {
        sharedPreferences(context).edit {
            putInt(context.getString(R.string.ocr_difference_length_threshold), threshold)
        }
    }

    fun savePlayers(context: Context, players: List<com.klamerek.fantasyrealms.game.Player>) {
        sharedPreferences(context).edit {
            putString(context.getString(R.string.saved_players), serializePlayers(players))
        }
    }

    fun loadPlayers(context: Context): List<com.klamerek.fantasyrealms.game.Player> {
        val sharedPref = sharedPreferences(context)
        val serialized = sharedPref.getString(context.getString(R.string.saved_players), null)
        return if (serialized != null) deserializePlayers(serialized) else emptyList()
    }

    fun saveDiscardArea(context: Context, game: com.klamerek.fantasyrealms.game.Game) {
        sharedPreferences(context).edit {
            putString(context.getString(R.string.saved_discard), serializeGame(game))
        }
    }

    fun loadDiscardArea(context: Context): com.klamerek.fantasyrealms.game.Game? {
        val sharedPref = sharedPreferences(context)
        val serialized = sharedPref.getString(context.getString(R.string.saved_discard), null)
        return if (serialized != null) deserializeGame(serialized) else null
    }

    private fun serializePlayers(players: List<com.klamerek.fantasyrealms.game.Player>): String {
        val jsonArray = org.json.JSONArray()
        players.forEach { player ->
            val jsonPlayer = org.json.JSONObject()
            jsonPlayer.put("name", player.name())
            jsonPlayer.put("game", serializeGame(player.game()))
            jsonArray.put(jsonPlayer)
        }
        return jsonArray.toString()
    }

    private fun deserializePlayers(serialized: String): List<com.klamerek.fantasyrealms.game.Player> {
        val players = mutableListOf<com.klamerek.fantasyrealms.game.Player>()
        try {
            val jsonArray = org.json.JSONArray(serialized)
            for (i in 0 until jsonArray.length()) {
                val jsonPlayer = jsonArray.getJSONObject(i)
                val name = jsonPlayer.getString("name")
                val gameSerialized = jsonPlayer.getString("game")
                val game = deserializeGame(gameSerialized)
                players.add(com.klamerek.fantasyrealms.game.Player(name, game))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return players
    }

    private fun serializeGame(game: com.klamerek.fantasyrealms.game.Game): String {
        val jsonGame = org.json.JSONObject()
        jsonGame.put("wildfireWithOutsiders", game.wildfireWithOutsiders)
        jsonGame.put("phoenixDeluxe", game.phoenixDeluxe)
        jsonGame.put("noScoring", game.noScoring)

        val handCards = org.json.JSONArray()
        game.handCards().forEach { handCards.put(it.definition.id) }
        jsonGame.put("handCards", handCards)

        val tableCards = org.json.JSONArray()
        game.cards().filter { it.definition.position() == com.klamerek.fantasyrealms.game.CardPosition.TABLE }
            .forEach { tableCards.put(it.definition.id) }
        jsonGame.put("tableCards", tableCards)

        val selections = org.json.JSONArray()
        com.klamerek.fantasyrealms.game.CardDefinitions.getAll().forEach { definition ->
            if (game.hasManualEffect(definition)) {
                val cardSelected = game.ruleEffectCardSelectionAbout(definition).firstOrNull()
                val suitSelected = game.ruleEffectSuitSelectionAbout(definition).firstOrNull()
                if (cardSelected != null || suitSelected != null) {
                    val jsonSelection = org.json.JSONObject()
                    jsonSelection.put("sourceId", definition.id)
                    cardSelected?.let { jsonSelection.put("cardSelectedId", it.id) }
                    suitSelected?.let { jsonSelection.put("suitSelected", it.name) }
                    selections.put(jsonSelection)
                }
            }
        }
        jsonGame.put("selections", selections)

        return jsonGame.toString()
    }

    private fun deserializeGame(serialized: String): com.klamerek.fantasyrealms.game.Game {
        val jsonGame = org.json.JSONObject(serialized)
        val wildfireWithOutsiders = jsonGame.optBoolean("wildfireWithOutsiders", false)
        val phoenixDeluxe = jsonGame.optBoolean("phoenixDeluxe", false)
        val noScoring = jsonGame.optBoolean("noScoring", false)
        val game = com.klamerek.fantasyrealms.game.Game(wildfireWithOutsiders, phoenixDeluxe, noScoring)

        val allCards = com.klamerek.fantasyrealms.game.CardDefinitions.getAllById()

        val handCards = jsonGame.getJSONArray("handCards")
        for (i in 0 until handCards.length()) {
            allCards[handCards.getInt(i)]?.let { game.add(it) }
        }

        val tableCards = jsonGame.optJSONArray("tableCards")
        if (tableCards != null) {
            for (i in 0 until tableCards.length()) {
                allCards[tableCards.getInt(i)]?.let { game.add(it) }
            }
        }

        val selections = jsonGame.optJSONArray("selections")
        if (selections != null) {
            for (i in 0 until selections.length()) {
                val jsonSelection = selections.getJSONObject(i)
                val sourceId = jsonSelection.getInt("sourceId")
                val cardSelectedId = if (jsonSelection.has("cardSelectedId")) jsonSelection.getInt("cardSelectedId") else null
                val suitSelectedName = if (jsonSelection.has("suitSelected")) jsonSelection.getString("suitSelected") else null

                val source = allCards[sourceId]
                val cardSelected = if (cardSelectedId != null) allCards[cardSelectedId] else null
                val suitSelected = if (suitSelectedName != null) com.klamerek.fantasyrealms.game.Suit.valueOf(suitSelectedName) else null

                game.applySelection(source, cardSelected, suitSelected)
            }
        }

        return game
    }


}
