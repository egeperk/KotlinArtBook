package com.egeperk.kotlinartbook

import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.egeperk.kotlinartbook.databinding.ActivityArtDetailBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_art_detail.*
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.jar.Manifest
import kotlin.math.max

class ArtDetail : AppCompatActivity() {

    private lateinit var binding: ActivityArtDetailBinding

    private lateinit var cursor : Cursor
    private lateinit var selectedImage : Bitmap
    private lateinit var database: SQLiteDatabase

    private var artId : Int = 0
    private lateinit var activityResultLauncher : ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var info : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtDetailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        registerLauncher()

        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null)

        val intent = intent
        val info = intent.getStringExtra("info")
        if (info.equals("new")) {
            binding.artName.setText("")
            binding.artistName.setText("")
            binding.yearTextView.setText("")
            binding.imageView.setImageResource(R.drawable.ic_launcher_background)
        } else {

            artId = intent.getIntExtra("artId", 1)
            binding.button.isVisible = false
            binding.artName.keyListener = null
            binding.artistName.keyListener = null
            binding.yearTextView.keyListener = null
            binding.imageView.isClickable = false
            cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", arrayOf(artId.toString()))

            val artNameIx = cursor.getColumnIndex("artname")
            val artistIx = cursor.getColumnIndex("artistname")
            val yearIx = cursor.getColumnIndex("year")
            val imageIx = cursor.getColumnIndex("image")

            while (cursor.moveToNext()) {
                binding.artName.setText(cursor.getString(artNameIx))
                binding.artistName.setText(cursor.getString(artistIx))
                binding.yearTextView.setText(cursor.getString(yearIx))

                val bytes = cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                binding.imageView.setImageBitmap(bitmap)
        }


            cursor.close()



        }


    }




    fun save (view: View) {
        val artName: String = binding.artName.text.toString()
        val artistName :String = binding.artistName.text.toString()
        val yearText : String = binding.yearTextView.text.toString()


        if(selectedImage != null) {
            val smallImage: Bitmap = makeSmallImage(selectedImage, 300)


            val outputStream = ByteArrayOutputStream()
            smallImage.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
            val byteArray: ByteArray = outputStream.toByteArray()

            try {
                database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY, artname VARCHAR, artistname VARCHAR, year VARCHAR, image BLOB)")

                val sqlString = "INSERT INTO arts(artname,artistname,year,image) VALUES(?,?,?,?)"
                val sqLiteStatement = database.compileStatement(sqlString)
                sqLiteStatement.bindString(1, artName)
                sqLiteStatement.bindString(2, artistName)
                sqLiteStatement.bindString(3, yearText)
                sqLiteStatement.bindString(4, byteArray.toString())
                sqLiteStatement.execute()

            } catch (e: Exception) {

            }

            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }

    private fun makeSmallImage(selectedImage: Bitmap, maximumSize: Int): Bitmap {
        var width : Int = selectedImage.width
        var height : Int = selectedImage.height

        val bitmapRatio : Double = width.toDouble()/height.toDouble()
        if (bitmapRatio >1) {
            width = maximumSize
            val scaldeHeight = width/bitmapRatio
            height = scaldeHeight.toInt()
        } else {
            height = maximumSize
            val scaledWidth = height * bitmapRatio
            width = scaledWidth.toInt()
        }

        return Bitmap.createScaledBitmap(selectedImage, width, height, true)
    }


    fun selectImage (view: View) {

        if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give permission", View.OnClickListener {
                    permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }).show()
            } else {
                permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {

            val intentToGallery: Intent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)

        }

    }

    private fun registerLauncher () {

        activityResultLauncher = registerForActivityResult(StartActivityForResult()) {
            result ->  if (result.resultCode == RESULT_OK) {
                val intentFromResult = result.data
            if (intentFromResult != null) {
                val imageData = intentFromResult.data!!

                try {
                    if (Build.VERSION.SDK_INT >= 28) {
                        val source : ImageDecoder.Source = ImageDecoder.createSource(this@ArtDetail.contentResolver,imageData)
                        selectedImage = ImageDecoder.decodeBitmap(source)
                        binding.imageView.setImageBitmap(selectedImage)
                    } else {
                        selectedImage = MediaStore.Images.Media.getBitmap(contentResolver, imageData)
                        binding.imageView.setImageBitmap(selectedImage)

                    }
                } catch (e: Exception) {

                }
            }
        }
        }
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            result -> if (result) {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)
        } else {
            Toast.makeText(this@ArtDetail, "Permission needed!", Toast.LENGTH_LONG).show()
        }
        }

    }





}