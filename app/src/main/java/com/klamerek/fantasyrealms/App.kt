package com.klamerek.fantasyrealms

import android.app.Application
import android.content.res.Resources
import androidx.annotation.StringRes
import com.klamerek.fantasyrealms.game.DiscardArea
import com.klamerek.fantasyrealms.game.Player
import com.klamerek.fantasyrealms.util.LocaleManager
import com.klamerek.fantasyrealms.util.Preferences

class App : Application() {
    companion object {
        lateinit var mResources: Resources
    }

    override fun onCreate() {
        super.onCreate()
        mResources = LocaleManager.updateContextWithPreferredLanguage(baseContext).resources
        
        Player.all.clear()
        Player.all.addAll(Preferences.loadPlayers(this))
        
        Preferences.loadDiscardArea(this)?.let { savedDiscardGame ->
            DiscardArea.instance.game().clear()
            DiscardArea.instance.game().update(savedDiscardGame.cards().map { it.definition })
            // Re-apply selections for discard area if any (though usually noScoring = true means no selections)
            savedDiscardGame.cards().forEach { card ->
                 savedDiscardGame.ruleEffectCardSelectionAbout(card.definition).firstOrNull()?.let { selected ->
                     DiscardArea.instance.game().applySelection(card.definition, selected)
                 }
            }
        }
    }

}

object Strings {
    fun get(@StringRes stringRes: Int, vararg formatArgs: Any = emptyArray()): String {
        return App.mResources.getString(stringRes, *formatArgs)
    }
}
