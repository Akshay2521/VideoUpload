package com.example.myapplication

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task


class MainActivity : AppCompatActivity() {
    lateinit var uploadv: Button
    var progressDialog: ProgressDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initialise layout
        uploadv = findViewById(R.id.uploadButton)
        uploadv.setOnClickListener(View.OnClickListener { // Code for showing progressDialog while uploading
            progressDialog = ProgressDialog(this@MainActivity)
            choosevideo()
        })
    }

    // choose a video from phone storage
    private fun choosevideo() {
        val intent = Intent()
        intent.type = "video/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, 5)
    }

    var videouri: Uri? = null

    // startActivityForResult is used to receive the result, which is the selected video.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 5 && resultCode == RESULT_OK && data != null && data.data != null) {
            videouri = data.data
            progressDialog!!.setTitle("Uploading...")
            progressDialog!!.show()
            uploadvideo()
        }
    }

    private fun getfiletype(videouri: Uri): String? {
        val r = contentResolver
        // get the file type ,in this case its mp4
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return mimeTypeMap.getExtensionFromMimeType(r.getType(videouri))
    }

    private fun uploadvideo() {
        if (videouri != null) {
            // save the selected video in Firebase storage
            val reference: StorageReference = FirebaseStorage.getInstance().getReference(
                "Files/" + System.currentTimeMillis() + "." + getfiletype(
                    videouri!!
                )
            )
            reference.putFile(videouri)
                .addOnSuccessListener(OnSuccessListener<Any> { taskSnapshot ->
                    val uriTask: Task<Uri> = taskSnapshot.getStorage().getDownloadUrl()
                    while (!uriTask.isSuccessful);
                    // get the link of video
                    val downloadUri = uriTask.result.toString()
                    val reference1: DatabaseReference =
                        FirebaseDatabase.getInstance().getReference("Video")
                    val map = HashMap<String, String>()
                    map["videolink"] = downloadUri
                    reference1.child("" + System.currentTimeMillis()).setValue(map)
                    // Video uploaded successfully
                    // Dismiss dialog
                    progressDialog!!.dismiss()
                    Toast.makeText(this@MainActivity, "Video Uploaded!!", Toast.LENGTH_SHORT).show()
                }).addOnFailureListener(OnFailureListener { e -> // Error, Image not uploaded
                    progressDialog!!.dismiss()
                    Toast.makeText(this@MainActivity, "Failed " + e.message, Toast.LENGTH_SHORT)
                        .show()
                }).addOnProgressListener(object : OnProgressListener<UploadTask.TaskSnapshot?>() {
                    // Progress Listener for loading
                    // percentage on the dialog box
                    fun onProgress(taskSnapshot: UploadTask.TaskSnapshot) {
                        // show the progress bar
                        val progress: Double =
                            100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount()
                        progressDialog!!.setMessage("Uploaded " + progress.toInt() + "%")
                    }
                })
        }
    }
}
