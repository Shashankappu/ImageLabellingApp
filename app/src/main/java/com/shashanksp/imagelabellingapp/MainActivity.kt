package com.shashanksp.imagelabellingapp

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class MainActivity : AppCompatActivity() {
    lateinit var ivPicture : ImageView
    lateinit var tvResult : TextView
    lateinit var btnChoosePicture : Button

    private val CAMERA_PERMISSION_CODE = 123
    private val READ_STORAGE_PERMISSION_CODE = 113
    private val  WRITE_STORAGE_PERMISSION_CODE= 113
    private val TAG = "myTag"
    private lateinit var cameraLauncher : ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher : ActivityResultLauncher<Intent>

    lateinit var inputImage : InputImage
    lateinit var imagelabler : ImageLabeler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivPicture = findViewById(R.id.ivPicture)
        tvResult = findViewById(R.id.tvResult)
        btnChoosePicture = findViewById(R.id.btnChoosePicture)

        imagelabler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            object : ActivityResultCallback<ActivityResult> {
                override fun onActivityResult(result: ActivityResult?) {
                    val data = result?.data
                    try{
                         val photo = data?.extras?.get("data") as Bitmap
                        ivPicture.setImageBitmap(photo)
                        inputImage = InputImage.fromBitmap(photo,0)
                        processImage()
                    }catch (e:Exception){
                        Log.d(TAG,"onActivityResult: ${e.message}")
                    }
                }
            }
        )

        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            object : ActivityResultCallback<ActivityResult> {
                override fun onActivityResult(result: ActivityResult?) {
                    val data = result?.data
                    try{
                        inputImage = InputImage.fromFilePath(this@MainActivity, data?.data!!)
                        ivPicture.setImageURI(data.data)
                        processImage()
                    }catch (e:Exception){

                    }
                }
            }
        )

        btnChoosePicture.setOnClickListener{
            val options = arrayOf("Camera","Gallery")
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Choose from ")
            builder.setItems(options, {
                dialog, which ->
                if(which==0){
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    cameraLauncher.launch(cameraIntent)
                }else{
                    val storageIntent = Intent()
                    storageIntent.setType("image/*")
                    storageIntent.setAction(Intent.ACTION_GET_CONTENT)
                    galleryLauncher.launch(storageIntent)
                }
            })
            builder.show()
        }
    }

    private fun processImage() {
        imagelabler.process(inputImage)
            .addOnSuccessListener {
                var result = ""
                for (label in it) {
                    result =result +"\n"+ label.text
                }
                tvResult.text = result
            }.addOnFailureListener{
                Log.d(TAG,"processImage: ${it.message}")
            }
    }

    override fun onResume() {
        super.onResume()

        checkPermission(android.Manifest.permission.CAMERA,CAMERA_PERMISSION_CODE)
    }
    private fun checkPermission(permission: String,requestCode : Int){
        if(ContextCompat.checkSelfPermission(this@MainActivity,permission)==PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission),requestCode)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == CAMERA_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE,READ_STORAGE_PERMISSION_CODE)
            }else{
                Toast.makeText(this@MainActivity,"Camera Permission Denied",Toast.LENGTH_SHORT).show()
            }
        }else if(requestCode == READ_STORAGE_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,WRITE_STORAGE_PERMISSION_CODE)
            }else{
                Toast.makeText(this@MainActivity,"Storage Permission Denied",Toast.LENGTH_SHORT).show()
            }
        }else if(requestCode == WRITE_STORAGE_PERMISSION_CODE){
            if(!(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED)){
                Toast.makeText(this@MainActivity,"Storage Permission Denied",Toast.LENGTH_SHORT).show()
            }
        }
    }
}