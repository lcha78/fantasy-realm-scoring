package com.klamerek.fantasyrealms.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.camera.core.*
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.klamerek.fantasyrealms.R
import com.klamerek.fantasyrealms.databinding.ActivityCardsSelectionBinding
import com.klamerek.fantasyrealms.databinding.ActivityOnTheFlyScanBinding
import com.klamerek.fantasyrealms.databinding.ChipDetectedItemBinding
import com.klamerek.fantasyrealms.game.CardDefinitions
import com.klamerek.fantasyrealms.ocr.CardTitleRecognizer
import com.klamerek.fantasyrealms.util.*
import androidx.camera.core.Camera as CameraX


/**
 * Same as ScanActivity but scanning "on the fly"
 */
@ExperimentalGetImage
class OnTheFlyScanActivity : CustomActivity(), CameraUseCase {

    private val cardsDetected = ArrayList<String>()
    private val cardDetectedSaveKey = "cardDetectedSaveKey"

    private lateinit var recognizer: CardTitleRecognizer
    private lateinit var binding: ActivityOnTheFlyScanBinding
    private lateinit var adapter: ChipsDetectedAdapter
    private var camera: CameraX? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getStringArrayList(cardDetectedSaveKey)
            ?.let { cardsDetected.addAll(it) }

        binding = ActivityOnTheFlyScanBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        manageCameraPermission(this)

        recognizer = CardTitleRecognizer(baseContext)

        updateNumberOfCardsLabel()

        binding.chipsView.layoutManager = LinearLayoutManager(this)
        val bindingForChips = ActivityCardsSelectionBinding.inflate(layoutInflater)
        val tagToChip = bindingForChips.chipGroup.children
            .filterIsInstance<Chip>()
            .onEach { it.isCheckable = false }
            .associateBy { it.tag.toString() }
        adapter = ChipsDetectedAdapter(tagToChip, cardsDetected)
        binding.chipsView.adapter = adapter

        @SuppressLint("NotifyDataSetChanged")
        val itemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(
            binding.chipsView.context, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) { position: Int ->
            cardsDetected.removeAt(position)
            adapter.notifyDataSetChanged()
        })
        itemTouchHelper.attachToRecyclerView(binding.chipsView)

        binding.buttonOk.setOnClickListener {
            val closingIntent = Intent()
            val answer = CardsSelectionExchange()
            answer.source = Constants.CARD_LIST_SOURCE_SCAN
            answer.cardsSelected.addAll(cardsDetected.map { it.toInt() })
            closingIntent.putExtra(Constants.CARD_SELECTION_DATA_EXCHANGE_SESSION_ID, answer)
            setResult(Constants.RESULT_OK, closingIntent)
            finishAfterTransition()
        }

        binding.flashButton.setOnClickListener {
            val flashMode = camera?.cameraInfo?.torchState?.value == TorchState.ON
            camera?.cameraControl?.enableTorch(!flashMode)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList(cardDetectedSaveKey, cardsDetected)
        super.onSaveInstanceState(outState)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateChipsDetected(cardIds: List<String>) {
        cardIds.forEach { cardId ->
            if (!cardsDetected.contains(cardId)) {
                cardsDetected.add(cardId)
                adapter.notifyDataSetChanged()
            }
        }
        updateNumberOfCardsLabel()
    }

    private fun updateNumberOfCardsLabel() {
        binding.numberOfCardsLabel.text = getString(R.string.x_cards, cardsDetected.size)
    }

    override fun getActivity(): ComponentActivity = this

    override fun getCameraPreview(): Preview.SurfaceProvider =
        binding.cameraPreview.surfaceProvider

    override fun getMainCameraUseCase(): UseCase {
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = convertImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                val allCardsById = CardDefinitions.getAllById()
                recognizer.process(image).addOnSuccessListener {
                    val cardIds = it.map { index -> allCardsById[index]?.id.toString() }
                    updateChipsDetected(cardIds)
                    imageProxy.close()
                }
            }
        }
        return imageAnalysis
    }

    override fun onCameraReady(camera: CameraX) {
        this.camera = camera
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResultDelegator(requestCode, this)
    }

}

class ChipsDetectedAdapter(
    private val chipByTag: Map<String, Chip>,
    private val chipTags: Collection<String>
) :
    RecyclerView.Adapter<ChipsDetectedAdapter.ChipHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipHolder {
        val itemBinding =
            ChipDetectedItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChipHolder(itemBinding)
    }

    override fun getItemCount(): Int = chipTags.size

    override fun onBindViewHolder(holder: ChipHolder, position: Int) {
        chipByTag[chipTags.elementAt(position)]?.let { holder.bindPlayer(it) }
    }

    class ChipHolder(v: ChipDetectedItemBinding) :
        RecyclerView.ViewHolder(v.root) {

        private var view: ChipDetectedItemBinding = v

        @SuppressLint("ClickableViewAccessibility")
        fun bindPlayer(chip: Chip) {
            (chip.parent as? ViewGroup)?.removeView(chip)
            view.mainLayout.removeAllViewsInLayout()
            view.mainLayout.addView(chip)
        }

    }

}
