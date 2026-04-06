package com.klamerek.fantasyrealms.screen

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.klamerek.fantasyrealms.BuildConfig
import com.klamerek.fantasyrealms.R
import com.klamerek.fantasyrealms.databinding.ActivityPlayerSelectionBinding
import com.klamerek.fantasyrealms.databinding.PlayerListItemBinding
import com.klamerek.fantasyrealms.game.DiscardArea
import com.klamerek.fantasyrealms.game.Game
import com.klamerek.fantasyrealms.game.Player
import com.klamerek.fantasyrealms.screen.PlayerSelectionActivity.Companion.playerNameDialog
import com.klamerek.fantasyrealms.util.Constants
import com.klamerek.fantasyrealms.util.Preferences
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Activity to add and remove players
 *
 */
class PlayerSelectionActivity : CustomActivity() {

    private lateinit var adapter: PlayerSelectionAdapter
    private lateinit var binding: ActivityPlayerSelectionBinding

    class PlayerNameDialog(context: Context, layoutInflater: LayoutInflater) {

        @Suppress("MagicNumber")
        private val delayBeforeShowingKeyboard = 200L

        val alertDialog: AlertDialog
        val field: TextInputEditText?
        private var okAction: Runnable? = null
        private val keyboard: InputMethodManager =
            getSystemService(context, InputMethodManager::class.java)!!

        init {
            val dialogView: View = layoutInflater.inflate(R.layout.dialog_new_player, null)
            field = dialogView.findViewWithTag("playerNameEditText")
            alertDialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton(R.string.ok_button) { _, _ -> okAction?.run() }
                .setNegativeButton(R.string.cancel_button) { _, _ -> }
                .create()
            alertDialog.setOnShowListener {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled =
                    !(field?.text?.isBlank() ?: true)
            }
            field?.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled =
                        !s.isNullOrBlank()
                }

            })
        }

        fun show(action: Runnable) {
            this.okAction = action
            this.alertDialog.show()
            playerNameDialog.field?.requestFocus()
            playerNameDialog.field?.selectAll()
            playerNameDialog.field?.postDelayed({
                keyboard.showSoftInput(playerNameDialog.field, 0)
            }, delayBeforeShowingKeyboard)
        }

    }

    companion object {
        lateinit var playerNameDialog: PlayerNameDialog
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun onResume() {
        super.onResume()
        updatePlayerList()
        binding.playersView.scheduleLayoutAnimation();
        binding.discardItem.scoreLabel.text =
            "" + DiscardArea.instance.game().actualHandSize() + " card(s)"
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        binding = ActivityPlayerSelectionBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setUpListeners()

        binding.discardItem.playerNameField.text = getString(R.string.discard_area)

        val linearLayoutManager = LinearLayoutManager(this)
        binding.playersView.addItemDecoration(
            DividerItemDecoration(
                binding.playersView.context,
                DividerItemDecoration.VERTICAL
            )
        )
        binding.playersView.layoutManager = linearLayoutManager
        adapter = PlayerSelectionAdapter(Player.all)
        binding.playersView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(
            binding.playersView.context, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) { position: Int ->
            val playerToDelete = adapter.playersList[position]
            val originalIndex = Player.all.indexOf(playerToDelete)
            EventBus.getDefault().post(PlayerDeletionEvent(originalIndex))
        })
        itemTouchHelper.attachToRecyclerView(binding.playersView)
    }

    private fun setUpListeners() {
        playerNameDialog = PlayerNameDialog(this, this.layoutInflater)

        binding.addPlayerButton.setOnClickListener {
            playerNameDialog.field?.text?.clear()
            playerNameDialog.field?.setText(Player.generateNextPlayerName())
            playerNameDialog.show {
                EventBus.getDefault().post(
                    PlayerCreationEvent(
                        playerNameDialog.field?.text?.toString()
                            ?: getString(R.string.new_player_default_value)
                    )
                )
            }
        }
        binding.discardItem.editButton.setOnClickListener {
            EventBus.getDefault().post(DiscardAreaEditEvent())
        }
        binding.clearButton.setOnClickListener {
            EventBus.getDefault().post(ClearAllScoresEvent())
        }
        binding.shareButton.setOnClickListener {
            shareScreenshot()
        }
    }

    private fun shareScreenshot() {
        val bitmap = Bitmap.createBitmap(
            binding.playerSelectionLayout.width,
            binding.playerSelectionLayout.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        binding.playerSelectionLayout.draw(canvas)

        try {
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            val stream = FileOutputStream("$cachePath/screenshot.png")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val imagePath = File(cacheDir, "images")
            val newFile = File(imagePath, "screenshot.png")
            val contentUri: Uri = FileProvider.getUriForFile(
                this,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                newFile
            )

            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.setDataAndType(contentUri, contentResolver.getType(contentUri))
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_button)))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun save() {
        Preferences.savePlayers(baseContext, Player.all)
        Preferences.saveDiscardArea(baseContext, DiscardArea.instance.game())
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updatePlayerList() {
        Player.all.forEach { it.game().calculate() }
        adapter.updateList(Player.all.sortedByDescending { it.game().score() })
    }

    @SuppressLint("NotifyDataSetChanged")
    @Subscribe
    fun addPlayer(event: PlayerCreationEvent) {
        runOnUiThread {
            Player.all.add(Player(event.name, Game(
                wildfireWithOutsiders = Preferences.getWildfireWithOutsiders(baseContext),
                phoenixDeluxe = Preferences.getPhoenixDeluxeEdition(baseContext)
            )))
            updatePlayerList()
            save()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Subscribe
    fun updatePlayer(event: PlayerUpdateEvent) {
        runOnUiThread {
            Player.all[event.index].setName(event.name)
            updatePlayerList()
            save()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Subscribe
    fun removePlayer(event: PlayerDeletionEvent) {
        runOnUiThread {
            Player.all.removeAt(event.index)
            updatePlayerList()
            save()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Suppress("UnusedPrivateMember")
    @Subscribe
    fun removeAllPlayers(event: AllPlayersDeletionEvent) {
        runOnUiThread {
            Player.all.clear()
            updatePlayerList()
            save()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Subscribe
    fun clearPlayers(event: ClearAllScoresEvent) {
        runOnUiThread {
            Player.all.forEach { it.game().clear() }
            updatePlayerList()
            save()
        }
    }

    @Subscribe
    fun editPlayer(event: PlayerEditEvent) {
        val handSelectionIntent = Intent(this, HandSelectionActivity::class.java)
        handSelectionIntent.putExtra(Constants.GAME_SESSION_ID, Player.all.indexOf(event.player))
        startActivity(handSelectionIntent)
    }

    @SuppressLint("NotifyDataSetChanged")
    @Suppress("UnusedPrivateMember")
    @Subscribe
    fun editDiscardArea(event: DiscardAreaEditEvent) {
        val handSelectionIntent = Intent(this, HandSelectionActivity::class.java)
        handSelectionIntent.putExtra(Constants.GAME_SESSION_ID, -1)
        startActivity(handSelectionIntent)
    }

}

class PlayerSelectionAdapter(initialPlayers: Collection<Player>) :
    RecyclerView.Adapter<PlayerSelectionAdapter.PlayerHolder>() {

    var playersList = initialPlayers.toList()

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<Player>) {
        playersList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerHolder {
        val itemBinding =
            PlayerListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlayerHolder(itemBinding)
    }

    override fun getItemCount(): Int = playersList.size

    override fun onBindViewHolder(holder: PlayerHolder, position: Int) {
        holder.bindPlayer(playersList[position])
    }

    class PlayerHolder(v: PlayerListItemBinding) :
        RecyclerView.ViewHolder(v.root) {

        private var view: PlayerListItemBinding = v

        @SuppressLint("ClickableViewAccessibility")
        fun bindPlayer(player: Player) {
            view.playerNameField.setOnLongClickListener {
                playerNameDialog.field?.setText(player.name())
                playerNameDialog.show {
                    EventBus.getDefault().post(
                        PlayerUpdateEvent(
                            Player.all.indexOf(player),
                            playerNameDialog.field?.text?.toString()
                                ?: player.name()
                        )
                    )
                }
                true
            }
            view.playerNameField.text = player.name()
            player.game().calculate()
            view.scoreLabel.text = player.game().score().toString()
            view.editButton.setOnClickListener {
                EventBus.getDefault().post(PlayerEditEvent(player))
            }
        }

    }

}

class PlayerCreationEvent(val name: String)

class PlayerUpdateEvent(val index: Int, val name: String)

class PlayerDeletionEvent(val index: Int)

class AllPlayersDeletionEvent

class PlayerEditEvent(val player: Player)

class DiscardAreaEditEvent

class ClearAllScoresEvent
