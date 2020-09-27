package com.app.summonpokemon

import android.content.Context
import android.widget.Toast
import com.google.ar.core.AugmentedImage
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.animation.ModelAnimator
import com.google.ar.sceneform.collision.Box
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import kotlin.math.max

class AugmentedImageNode(private val context: Context) : AnchorNode() {

    var image: AugmentedImage? = null

    //every 3d is model is generated in bg , we can use it to accept only when finish laoding
    private var modelCompletableFuture = ModelRenderable.builder().setSource(
        context,
        R.raw.beedrill
    ).build()

    private lateinit var renderable: ModelRenderable

    //assign image to node
    fun setAugmentedImage(image: AugmentedImage) {
        this.image = image

        if (!modelCompletableFuture.isDone) {
            modelCompletableFuture.thenAccept {

                setAugmentedImage(image)
            }.exceptionally {
                Toast.makeText(context, "Error creating renderable", Toast.LENGTH_SHORT).show()
                null
            }

            return
        }

        renderable = modelCompletableFuture.get()

        anchor =
            image.createAnchor(image.centerPose) //pose tell where is the object and where object is looking

        //for 3d model for adjust the scale because images or book cover has defined size
        val modelNode = Node().apply {
            setParent(this@AugmentedImageNode)
            renderable = this@AugmentedImageNode.renderable
        }

        val renderableBox = renderable.collisionShape as Box
        val maxEdgeSize = max(renderableBox.size.x,renderableBox.size.z)

        val maxImageEdge = max(image.extentX,image.extentZ)
        val modelScale = (maxImageEdge/maxEdgeSize)/2f

        modelNode.localScale = Vector3(modelScale,modelScale,modelScale)

        startAnimation()
    }

    //for applying animation to 3d model
    private fun startAnimation(){
        if(renderable.animationDataCount == 0){
            //if don't have animation
            return
        }

        val animationData = renderable.getAnimationData("Beedrill_Animation")
        ModelAnimator(animationData,renderable).apply {
            repeatCount = ModelAnimator.INFINITE
            start()
        }

    }
}