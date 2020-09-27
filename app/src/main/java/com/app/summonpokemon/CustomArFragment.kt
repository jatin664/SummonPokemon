package com.app.summonpokemon

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.ux.ArFragment
import java.io.IOException

//toggle if we want to use database or user decide his own image
private const val USE_DATABASE = true

class CustomArFragment : ArFragment() {

    private val TAG = this.javaClass.simpleName
    private val REQUEST_CODE_CHOOSE_IMAGE: Int = 100

    override fun onAttach(context: Context) {
        super.onAttach(context)

        //if we get image from gallery instead
        if(!USE_DATABASE){
            /*
             If you want to use your own photos, then
              use App Photoscan by google which makes sure book cover fill up the whole made
              Photo become much recognizable
          */
            chooseNewImage()
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //hide surface discovery
        planeDiscoveryController.hide()
        planeDiscoveryController.setInstructionView(null) // stop hand animation at start

        arSceneView.planeRenderer.isEnabled = false //disable dots

    }

    override fun getSessionConfiguration(session: Session?): Config {
        //to enable to autofocus
        val config = super.getSessionConfiguration(session)
        config.focusMode = Config.FocusMode.AUTO

        if(USE_DATABASE){
            config.augmentedImageDatabase = createAugImageDb(session ?: return config)
        }

        return config
    }

    //to choose image from gallery
    private fun chooseNewImage(){
        Intent(Intent.ACTION_GET_CONTENT).run {
            type = "image/*"
            startActivityForResult(this,REQUEST_CODE_CHOOSE_IMAGE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == Activity.RESULT_CANCELED){
            return
        }

        if(resultCode == Activity.RESULT_OK){
            if(requestCode == REQUEST_CODE_CHOOSE_IMAGE){
                val imageUrl = data?.data ?: return
                val session  = arSceneView.session ?: return //session will be used to configure augmented database (database of images that
                // should be detected in your app). We need to create a database for user image selection

                val config = getSessionConfiguration(session)
                val database = createAugmentedImageDatabaseWithSingleImage(session,imageUrl)
                config.augmentedImageDatabase = database
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE //way how ar fragment updates the camera
                session.configure(config)
            }

        }
    }

    private fun createAugImageDb(session: Session) : AugmentedImageDatabase?{
        return try{
            val inputStream = resources.openRawResource(R.raw.my_image_db)
            AugmentedImageDatabase.deserialize(session,inputStream)
        }
        catch (e:Exception){
            Log.d(TAG,"Exception $e")
            null
        }
    }

    private fun createAugmentedImageDatabaseWithSingleImage(session: Session, imageUrl: Uri): AugmentedImageDatabase? {
        val database = AugmentedImageDatabase(session)
        val bmp = loadAugBitmap(imageUrl)
        database.addImage("MyImage.jpg",bmp)
        return database
    }

    private fun loadAugBitmap(imageUrl: Uri): Bitmap? {
        return try{
            context?.contentResolver?.openInputStream(imageUrl)?.use {
                BitmapFactory.decodeStream(it)
            }
        }
        catch (e:IOException){
            Log.d(TAG,"Exception: $e")
            return null
        }
    }

}