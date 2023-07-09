package com.aiemail.superemail.Activities

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.database.Cursor
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Patterns
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import com.aiemail.superemail.Bottomsheet.ActionBottomDialogFragment
import com.aiemail.superemail.Dialog.MyCustomDialog
import com.aiemail.superemail.Models.Email
import com.aiemail.superemail.Models.FullMessage
import com.aiemail.superemail.MyApplication
import com.aiemail.superemail.R
import com.aiemail.superemail.chinalwb.are.Text_Operations.DemoUtil
import com.aiemail.superemail.databinding.ActivityTemplateBinding
import com.aiemail.superemail.utilis.PreferenceManager
import com.aiemail.superemail.viewmodel.FullMessagelViewmodel
import com.bumptech.glide.Glide
import com.chinalwb.are.android.inner.Html
import com.chinalwb.are.styles.toolbar.IARE_Toolbar
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_AlignmentCenter
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_AlignmentLeft
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_AlignmentRight
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_At
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_BackgroundColor
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_Bold
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_Hr
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_Image
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_Italic
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_Link
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_ListBullet
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_ListNumber
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_Quote
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_Strikethrough
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_Subscript
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_Superscript
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_Underline
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_Video
import com.chinalwb.are.styles.toolitems.IARE_ToolItem
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.MessagePartHeader
import com.google.api.services.gmail.model.ModifyMessageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.activation.DataHandler
import javax.activation.DataSource
import javax.activation.FileDataSource
import javax.mail.Multipart
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

class TemplateActivity : AppCompatActivity(), ActionBottomDialogFragment.ItemClickListener, MyCustomDialog.DialogListner {
    lateinit var bottomSheetDialog: BottomSheetDialog
    var preferenceManager: PreferenceManager? = null
    private lateinit var binding: ActivityTemplateBinding
    private lateinit var mToolbar: IARE_Toolbar
    var click: Boolean = false
    private val TIMEOUT = 1500
    val watermark = "Sent With SuperMail"
    var messageId: String = ""
    var sender_id: String = ""
    var realObject: Email = Email()

    private val PERMISSION_CODE = 1000
    private val IMAGE_CAPTURE_CODE = 1001

    var data: String = ""

    companion object{
        public var fromsnooze: Boolean = false
    }

    val fullmessagemodel: FullMessagelViewmodel by viewModels() {
        FullMessagelViewmodel.Factory(
            (application as MyApplication), (application as MyApplication).fullmessagerepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTemplateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mToolbar = binding.areToolbar

        // Generate a random ID using UUID
        val randomID = UUID.randomUUID().toString()
        sender_id = randomID
        if (intent.extras != null) {
            if (intent.extras!!.getBoolean("fromsend")!!.equals(true)) {
                sender_id = intent.extras!!.getString("senderid")!!
                Thread {
                    sendEmailFullmessage(fullmessagemodel, sender_id)
                }.start()
                return
            }
            data = intent.extras!!.getString("data")!!
            binding.email.setText(data)
        } else binding.email.setText("New message")

        fullmessagemodel.getEmailList().observe(this)
        {
//            for (i in it.indices)
//                Log.d(
//                    "onCreate",
//                    "sendEmailFullmessage: " + it.get(i).sender_id + "??" + it.get(i).content + ">>" + it.get(
//                        i
//                    ).sender
//                )
        }
        //Intialization of all views
        Initialization()

        ItemClick()
    }
    private fun Initialization() {
        preferenceManager = com.aiemail.superemail.utilis.PreferenceManager(this)
        preferenceManager!!.SetUpPreference()
        mToolbar = findViewById(R.id.areToolbar)
        val bold: IARE_ToolItem = ARE_ToolItem_Bold()
        val italic: IARE_ToolItem = ARE_ToolItem_Italic()
        val underline: IARE_ToolItem = ARE_ToolItem_Underline()
        val strikethrough: IARE_ToolItem = ARE_ToolItem_Strikethrough()
        val quote: IARE_ToolItem = ARE_ToolItem_Quote()
        val listNumber: IARE_ToolItem = ARE_ToolItem_ListNumber()
        val color: IARE_ToolItem = ARE_ToolItem_BackgroundColor()
        val listBullet: IARE_ToolItem = ARE_ToolItem_ListBullet()
        val hr: IARE_ToolItem = ARE_ToolItem_Hr()
        val link: IARE_ToolItem = ARE_ToolItem_Link()
        val subscript: IARE_ToolItem = ARE_ToolItem_Subscript()
        val superscript: IARE_ToolItem = ARE_ToolItem_Superscript()
        val left: IARE_ToolItem = ARE_ToolItem_AlignmentLeft()
        val center: IARE_ToolItem = ARE_ToolItem_AlignmentCenter()
        val right: IARE_ToolItem = ARE_ToolItem_AlignmentRight()
        val image: IARE_ToolItem = ARE_ToolItem_Image()
        val video: IARE_ToolItem = ARE_ToolItem_Video()
        val at: IARE_ToolItem = ARE_ToolItem_At()
        mToolbar.addToolbarItem(bold)
        mToolbar.addToolbarItem(italic)
        mToolbar.addToolbarItem(underline)
        mToolbar.addToolbarItem(strikethrough)
        mToolbar.addToolbarItem(quote)
        mToolbar.addToolbarItem(color)
        mToolbar.addToolbarItem(listNumber)
        mToolbar.addToolbarItem(listBullet)
        mToolbar.addToolbarItem(hr)
        mToolbar.addToolbarItem(link)
        mToolbar.addToolbarItem(subscript)
        mToolbar.addToolbarItem(superscript)
        mToolbar.addToolbarItem(left)
        mToolbar.addToolbarItem(center)
        mToolbar.addToolbarItem(right)
        mToolbar.addToolbarItem(image)
        mToolbar.addToolbarItem(video)
        mToolbar.addToolbarItem(at)

        binding.editText.setToolbar(mToolbar)
        //initToolbarArrow()
    }

    private fun ItemClick() {

        binding.bottombar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.clock1 -> MyCustomDialog(
                    R.layout.layout_snooze_dialog,
                    sender_id,
                    this,
                    this,
                    Message()
                ).show(
                    supportFragmentManager, "MyCustomFragment"
                )

                R.id.text1 -> {
                    binding.bottombar.visibility = View.GONE
                    binding.tool.visibility = View.VISIBLE
                }

                R.id.folder -> showBottomSheetpurchse()
                R.id.attach1 -> {
                    var bottomSheetDialog =
                        BottomSheetDialog(this, R.style.BottomSheetDialogcustom);
                    bottomSheetDialog.setContentView(R.layout.layout_attachment);
                    //  bottomSheetDialog!!.getWindow()?.setBackgroundDrawableResource(R.drawable.round_corner);
                    bottomSheetDialog.show()
                }


            }
            true
        }
        binding.back.setOnClickListener {
            onBackPressed()
        }

        binding.apply.setOnClickListener {
            binding.option.visibility = View.VISIBLE
            binding.tool.visibility = View.GONE
            val html: String = binding.editText.getHtml() + " " + watermark

            DemoUtil.saveHtml(this, html)
        }

        binding.bb.setOnClickListener {
            if (!click) {
                click = !click
                binding.group1.visibility = (View.VISIBLE);
                binding.group2.visibility = (View.VISIBLE);
                ViewCompat.animate(binding.bb).alpha(1f).setDuration(800).setListener(null);
            } else {
                click = !click
                binding.group1.visibility = (View.GONE);
                binding.group2.visibility = (View.GONE);
                ViewCompat.animate(binding.bb).alpha(1f).setDuration(800).setListener(null);
            }
        }
        binding.attach.setOnClickListener {
            bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogcustom);
            bottomSheetDialog.setContentView(R.layout.layout_attachment);
            bottomSheetDialog.findViewById<ImageView>(R.id.file_attach)!!.setOnClickListener {
                createFile()
            }
            bottomSheetDialog.findViewById<ImageView>(R.id.image)!!.setOnClickListener {
                createImage()
            }
            bottomSheetDialog.findViewById<LinearLayout>(R.id.camera)!!.setOnClickListener {
                val cameraPermission = Manifest.permission.CAMERA
                if (ContextCompat.checkSelfPermission(
                        this, cameraPermission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.CAMERA),
                        PERMISSION_CODE
                    );
                } else createCameraImage()
            }
            //  bottomSheetDialog!!.getWindow()?.setBackgroundDrawableResource(R.drawable.round_corner);
            bottomSheetDialog.show()

        }

        binding.file.setOnClickListener {
            showBottomSheetpurchse()
        }
        binding.text.setOnClickListener {
            binding.option.visibility = View.GONE
            binding.tool.visibility = View.VISIBLE
        }

        binding.clock.setOnClickListener {
            MyCustomDialog(
                R.layout.layout_snooze_dialog,
                sender_id,
                this,
                this,
                Message()
            ).show(
                supportFragmentManager, "MyCustomFragment"
            )

        }
        binding.send.setOnClickListener {
            MyCustomDialog(R.layout.layout_send_later, sender_id, this, this, Message()).show(
                supportFragmentManager, "MyCustomFragment"
            )
        }
        binding.add.setOnClickListener {
            MyCustomDialog(R.layout.layout_common_dialog, sender_id, this, this, Message()).show(
                supportFragmentManager, "MyCustomFragment"
            )
        }

        binding.pin.setOnClickListener {
            Signature()
            if (data.equals("forward")) {
                forwardEmail(realObject.msgid, binding.editext.text!!.trim().toString())
            } else {
                Thread() {
                    sendEmail()
                }.start()
            }
        }


    }

    private fun Signature() {
        var supermail = preferenceManager!!.getBoolean("supermail")
        var signature_custom = preferenceManager!!.getBoolean("signature_custom")
        var signature = preferenceManager!!.getString("signature")
        Log.d("TAG", "Signature: " + signature)
        if (signature_custom.equals(true) && signature != "") {
            //binding.signature.visibility = View.VISIBLE
            // Add the input filter to the EditText

// Set the signature to be appended
            if (supermail.equals(true))
                appendSignature(signature,"Sent with Supermail")
            else
            {
                appendSignature(signature,"")
            }
        } else if (signature_custom.equals(true)){
            appendSignature("","Sent with Supermail")
            // binding.signature.visibility = View.GONE
        }else
        {
            appendSignature("","")
        }
        binding.editText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {

                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

//        binding.sub.sets
    }

    private fun appendSignature(signature: String, supermail: String) {
        val signatureText = signature
        val currentText = binding.editText.text.toString()
        val newText = if (currentText.isNotBlank()) {
            "$currentText\n\n${binding.yourName.text.trim()}\n\n$signatureText\n\n\n$supermail"
        } else {
            signatureText
        }
        binding.editText.setText(newText)
    }

    private fun sendEmail() {
        var myPref = getSharedPreferences("APP_SHARED_PREF", Context.MODE_PRIVATE);
        var username = myPref.getString("username", "");
        var subject = binding.subEdit.text.trim()
        var messageBody = Html.fromHtml(Html.fromHtml(binding.editText.html).toString())
        var to = binding.editext.text!!.trim()
        if (Patterns.EMAIL_ADDRESS.matcher(to).matches()) {
            val email1 = createEmail(to.toString(), "" + subject, "" + messageBody)
            val message = createMessageWithEmail(email1)


            // Build a new authorized API client service.
            val httpTransport = NetHttpTransport().createRequestFactory { request ->
                request.connectTimeout = TIMEOUT
                request.readTimeout = TIMEOUT
            }.transport

            val credential1 = GoogleAccountCredential.usingOAuth2(
                this, setOf<String>(GmailScopes.GMAIL_READONLY)
            ).setSelectedAccountName(username)
            try {
//            credential1.token.refr
            } catch (e: IOException) {
                Log.e("TAG", e.message!!)

            }
            val gmailService = Gmail.Builder(
                httpTransport, JacksonFactory(), credential1
            ).setApplicationName("Spark").build()
            GlobalScope.launch {

                messageId = System.currentTimeMillis().toString()
                var fullMessage: FullMessage = FullMessage()
                fullMessage.sender = username;
                fullMessage.recipient = to.toString()
                fullMessage.subject = subject.toString()
                fullMessage.content = messageBody.toString()
                fullMessage.sender_id = sender_id
                fullMessage.imageResources = imageResources
                fullMessage.fileUriString = fileUriString.toString()

                fullmessagemodel.insertMail(fullMessage)

                if (!DirectComposeActivity.fromsnooze.equals(true)) {
                    val response = gmailService.users().messages().send(username, message).execute()
                    if (response != null) {

                        runOnUiThread {
                            Toast.makeText(
                                this@TemplateActivity,
                                "Email sent",
                                Toast.LENGTH_LONG
                            )
                                .show()
                            finish()
                        }

                    } else {
                        // Email sending failed
                        System.out.println("Failed to send the email.");

                        Log.d("TAG", "fetchMails: " + "failed")
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@TemplateActivity,
                            "Email Sceduleded",
                            Toast.LENGTH_LONG
                        )
                            .show()
                        finish()
                    }
                }
            }
        } else {
            runOnUiThread {
                Toast.makeText(this@TemplateActivity, "Enter Email id", Toast.LENGTH_LONG)
                    .show()
            }

        }


    }
    private fun createMessageWithEmail(email: MimeMessage): Message {
        val buffer = ByteArrayOutputStream()
        email.writeTo(buffer)
        val bytes = buffer.toByteArray()
        val encodedEmail = Base64.getUrlEncoder().encodeToString(bytes)
        val message = Message()
        message.raw = encodedEmail
        return message
    }

    //documents selecting
    private fun createFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type =
            "*/*" // Set the MIME type of files to be selected (you can set a specific type if required)
        getResult.launch(intent)
    }

    //images selection
    private fun createImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type =
            "image/*" // Set the MIME type of files to be selected (you can set a specific type if required)
        getResultImage.launch(intent)
    }

    //Camera intent to capture image
    private fun createCameraImage() {
        openCamera()

    }

    var image_uri: Uri? = null

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")

        //camera intent
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // set filename
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var vFilename = "FOTO_" + timeStamp + ".jpg"

        // set direcory folder
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), vFilename);
        image_uri = FileProvider.getUriForFile(
            this, this.getApplicationContext().getPackageName() + ".provider", file
        );
        Log.d("TAG", "onActivityResult: " + image_uri)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
    }

    var fileUriString: Uri? = null
    var imageResources: ArrayList<File> = arrayListOf()

    private val getResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            if (it.data != null) {
                // Retrieve the selected file URI
                val fileUri: Uri = it.data!!.data!!
                Log.d("TAG", "attachedfile: " + fileUri)
                fileUriString = fileUri
                imageResources.add(File(fileUriString.toString()))
                attachImages(imageResources)
                // Use the file URI as needed (e.g., send it in an email)
                // ...
                if (bottomSheetDialog != null && bottomSheetDialog.isShowing) bottomSheetDialog.dismiss()

            }
        }
    }
    private val getResultImage = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            if (it.data != null) {
                // Retrieve the selected file URI
                var selectedImageUri: Uri? = it.data!!.data
                // Get the path from the Uri
                // Get the path from the Uri
                val path = getPathFromURI(selectedImageUri)
                if (path != null) {
                    val f = File(path)
                    fileUriString = Uri.fromFile(f)
                    imageResources.add(File(fileUriString.toString()!!))
                    attachImages(imageResources)
                    if (bottomSheetDialog != null && bottomSheetDialog.isShowing) bottomSheetDialog.dismiss()
                }

                // Use the file URI as needed (e.g., send it in an email)
                // ...
            }
        }
    }

    fun getPathFromURI(contentUri: Uri?): String? {
        var res: String? = null
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = contentResolver.query(contentUri!!, proj, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val column_index: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                res = cursor.getString(column_index)
            }
        }
        if (cursor != null) {
            cursor.close()
        }
        return res
    }

    //Email creates with sender,receiver and all attachments
    private fun createEmail(to: String, subject: String, messageBody: String): MimeMessage {

        val attachmentFilePath = applicationContext.assets.open("pdf.docx")
        var bcc = "" + binding.bB.text
        var cc = "" + binding.cc.text
//        var to = "" + binding.editext.text
        val props = Properties()
        val session = Session.getDefaultInstance(props, null)

        val email = MimeMessage(session)
        email.setFrom(InternetAddress())
        email.addRecipient(javax.mail.Message.RecipientType.TO, InternetAddress(to))
        if (!bcc.length.equals(0)) {
            email.addRecipient(javax.mail.Message.RecipientType.BCC, InternetAddress(bcc))

        }
        if (!cc.length.equals(0)) {
            email.addRecipient(javax.mail.Message.RecipientType.CC, InternetAddress(cc))

        }
        email.subject = subject

        val bodyPart = MimeBodyPart()
        bodyPart.setContent(messageBody, "text/plain")

        val attachmentBodyPart = MimeBodyPart()

        var assetManager: AssetManager = assets
        val attachmentFileName = "pdf.docx"
        val inputStream: InputStream = assetManager.open(attachmentFileName)
        val source: DataSource = ByteArrayDataSource(inputStream, "application/octet-stream")
        attachmentBodyPart.dataHandler = DataHandler(source)
        if (fileUriString != null) {
            val pdfDocument = convertUriToPdfDocument(fileUriString!!)
            // attachmentBodyPart.fileName = RetrieveFile(pdfDocument!!).absolutePath
        }

        val multipart: Multipart = MimeMultipart("mixed")
        multipart.addBodyPart(bodyPart)

        // Add attachments to the multipart
        // Add attachments to the multipart
        for (file in imageResources) {
            val attachmentPart = MimeBodyPart()
            val fileDataSource = FileDataSource(file)
            attachmentPart.dataHandler = DataHandler(fileDataSource)
            attachmentPart.fileName = file.name
            multipart.addBodyPart(attachmentPart)
        }


        email.setContent(multipart)

        return email
    }

    //multiple images add to imageview
    fun attachImages(imageResources: ArrayList<File>) {
        // Iterate through the array and create ImageView elements dynamically

        for (imageRes in imageResources) {

            val imageView = ImageView(this)
            // Load the image into the ImageView using Glide
            Glide.with(this).load(imageRes).override(50, 50).into(imageView)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.image_width),
                resources.getDimensionPixelSize(R.dimen.image_height)
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.image_margin)
            }

            // Add the ImageView to the LinearLayout
            binding.imagecontain.addView(imageView)
        }
    }

    private fun convertUriToPdfDocument(uri: Uri): PdfDocument? {
        var pdfDocument = PdfDocument()
        try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
            if (parcelFileDescriptor != null) {
                val fileDescriptor = parcelFileDescriptor.fileDescriptor

                // Load the PDF document from the file descriptor
                //  pdfDocument = PdfDocumentAdapter.loadDocument(fileDescriptor)

                // Close the file descriptor
                //parcelFileDescriptor.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return pdfDocument
    }

    fun sendEmailFullmessage(viewmodel: FullMessagelViewmodel, sender_id: String) {
        CoroutineScope(Dispatchers.Main).launch(Dispatchers.IO) {
            // Fetch data from Room database on a background thread
//            Log.d("TAG", "sendEmailFullmessage: " + sender_id)
            val user = viewmodel.getEmail(sender_id)


            // Perform additional background fetch or processing
            // ...

            withContext(Dispatchers.Main) {
                user.observe(this@TemplateActivity)
                {
                    val user = it
//                    Log.d("TAG", "sendEmailFullmessage: " + user)
                    if (user != null) {
                        Thread {

                            sendEmailFullmessage_(MyApplication.instance.context!!, user)
                        }.start()
                    }
                }
                // Update UI with the fetched data

            }
        }

    }

    private fun sendEmailFullmessage_(context: Context, fullMessage: FullMessage) {
        Log.d(
            "sendEmailFullmessage_",
            "sendEmailFullmessage_: " + fullMessage.sender + ">" + fullMessage.content + ">" + fullMessage.subject + ">>" + fullMessage.recipient + ">>" + fullMessage.sender_id
        )
        var myPref = context.getSharedPreferences("APP_SHARED_PREF", Context.MODE_PRIVATE);
        var username = myPref.getString("username", "");


        // Build a new authorized API client service.
        val httpTransport = NetHttpTransport().createRequestFactory { request ->
            request.connectTimeout = TIMEOUT
            request.readTimeout = TIMEOUT
        }.transport

        val credential1 = GoogleAccountCredential.usingOAuth2(
            context, setOf<String>(GmailScopes.GMAIL_READONLY)
        ).setSelectedAccountName(username)
        try {

        } catch (e: IOException) {
            Log.e("TAG", e.message!!)

        }
        imageResources = fullMessage.imageResources
        fileUriString = Uri.parse(fullMessage.fileUriString)
        val email1 =
            createEmail(fullMessage.recipient!!, "" + fullMessage.subject, "" + fullMessage.content)
        val message = createMessageWithEmail(email1)

        val gmailService = Gmail.Builder(
            httpTransport, JacksonFactory(), credential1
        ).setApplicationName("Spark").build()


        val response =
            gmailService.users().messages().send(username, message).execute()
        if (response != null) {
            // Email sending was successful
            Toast.makeText(this, "Email Sent Successfully..", Toast.LENGTH_LONG).show()
            finish()

        } else {
            // Email sending failed
            finish()
        }


    }
    fun showBottomSheetpurchse() {

        val addPhotoBottomDialogFragment =
            ActionBottomDialogFragment.newInstance(R.layout.purchase_detail_layout)
        addPhotoBottomDialogFragment.show(
            supportFragmentManager, ActionBottomDialogFragment.TAG

        )

    }

    private fun forwardEmail(messageId: String, recipientEmail: String) {

        Thread {
            try {
                if (Patterns.EMAIL_ADDRESS.matcher(recipientEmail).matches()) {
                    var myPref = getSharedPreferences("APP_SHARED_PREF", Context.MODE_PRIVATE);
                    var username = myPref.getString("username", "");
                    // Build a new authorized API client service.
                    val httpTransport = NetHttpTransport().createRequestFactory { request ->
                        request.connectTimeout = TIMEOUT
                        request.readTimeout = TIMEOUT
                    }.transport

                    val credential1 = GoogleAccountCredential.usingOAuth2(
                        this, setOf<String>(GmailScopes.GMAIL_READONLY)
                    ).setSelectedAccountName(username)
                    try {
                        try {
                            val service =
                                Gmail.Builder(httpTransport, JacksonFactory(), credential1)
                                    .setApplicationName("Spark").build()
                            // Create a ModifyMessageRequest to add the FORWARD and remove the INBOX label
                            val forwardRequest =
                                ModifyMessageRequest().setAddLabelIds(listOf("FORWARD"))
                                    .setRemoveLabelIds(listOf("INBOX"))

                            // Apply the FORWARD and remove the INBOX label to simulate forwarding the email
                            service.users().messages().modify("me", messageId, forwardRequest)
                                .execute()

                            // Get the original email
                            val originalMessage: Message =
                                service.users().messages().get("me", messageId).execute()

                            // Create a new email for forwarding
                            val forwardMessage = Message()
                            Log.e("TAG", "forwardEmail: " + messageId)
                            forwardMessage.raw = originalMessage.raw
                            forwardMessage.labelIds =
                                listOf("INBOX") // Add INBOX label to the forwarded email
                            forwardMessage.threadId =
                                originalMessage.threadId // Set the same thread ID

                            // Set the new recipient as the "To" field
                            forwardMessage.payload = originalMessage.payload
                            forwardMessage.payload.headers =
                                updateRecipient(originalMessage.payload.headers, recipientEmail)

                            // Send the forward email
                            var response =
                                service.users().messages().send("me", forwardMessage).execute()
                            if (response != null) {
                                // Email sending was successful
                                runOnUiThread {
                                    Toast.makeText(
                                        this@TemplateActivity, "Email sent", Toast.LENGTH_LONG
                                    ).show()
                                }
                                Log.d("TAG", "fetchMails: " + "sent")
                            } else {
                                // Email sending failed
                                System.out.println("Failed to send the email.");

                                Log.d("TAG", "fetchMails: " + "failed")
                            }
                        } catch (e: GoogleJsonResponseException) {
                            e.printStackTrace()
                        } catch (e: UserRecoverableAuthIOException) {
                            e.printStackTrace()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    } catch (e: IOException) {
                        Log.e("TAG", e.message!!)

                    }
                }
                runOnUiThread {
                    Toast.makeText(this, "Enter valid Email  id", Toast.LENGTH_LONG).show()
                }


            } catch (e: Exception) {


            }


        }.start()

    }

    private fun updateRecipient(
        headers: List<MessagePartHeader>, newRecipient: String
    ): List<MessagePartHeader>? {
        for (header in headers) {
            if (header.name == "To") {
                header.value = newRecipient
                break
            }
        }
        return headers
    }
    override fun onItemClick(item: String?) {
    }

    override fun dialogClick(sender_id: String, dialog: MyCustomDialog) {
        fromsnooze = true
//        fullmessage.isSnoozed = true

        //Log.d("sendEmailFullmessage", "dialogClick: " + sender_id)
//        sender_id = sender_id
        //  fullmessagemodel.insertMail(fullmessage_)

        dialog.dismiss()
    }
}