package com.spyglass.alprlaunchersdksample.ui.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ScrollView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.spyglass.alprlaunchersdk.data.ALPRLauncherBuilder
import com.spyglass.alprlaunchersdk.data.CameraType
import com.spyglass.alprlaunchersdk.data.HotlistSourceType
import com.spyglass.alprlaunchersdk.data.ResponseType
import com.spyglass.alprlaunchersdk.launcher.ALPRLauncher
import com.spyglass.alprlaunchersdk.listener.LauncherListener
import com.spyglass.alprlaunchersdksample.BuildConfig
import com.spyglass.alprlaunchersdksample.ui.viewmodel.MainViewModel
import com.spyglass.alprlaunchersdksample.R
import com.spyglass.alprlaunchersdksample.ui.model.ResponseMode
import com.spyglass.alprlaunchersdksample.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity(), LauncherListener {

    // View binding instance
    private lateinit var binding: ActivityMainBinding

    // ViewModel instance
    private lateinit var viewModel: MainViewModel

    // ALPR Launcher components
    private val alprLauncherBuilder = ALPRLauncherBuilder()
    private val alprLauncher = ALPRLauncher

    // State variables for configuration
    private var hotlistSourceType = HotlistSourceType.PASSED_ONLY
    private var cameraType = CameraType.DEVICE_CAMERA
    private var responseType = ResponseType.ALL_ALERTS
    private var responseMode = ResponseMode.SINGLE
    private var hotlistDatabaseFile: File? = null
    private var hotlistDatabaseUri: Uri? = null
    private var table: String? = null

    // StringBuilder for logs
    private val logBuilder = StringBuilder()

    // Database selection result launcher
    private val selectDatabaseLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleDatabaseSelectionResult(result)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // Initialize UI components
        initUI()

        // Copy test database from assets
        viewModel.copyDatabaseFromAssets()

        // Initialize ALPR launcher
        alprLauncher.initLauncher(this, this)

        // Set up launch button listener
        binding.btnLaunch.setOnClickListener {
            launchALPRScanner()
        }
    }

    private fun initUI() {
        setupDropdowns()
    }

    // Setup all dropdowns and their listeners
    private fun setupDropdowns() {
        setupDatabaseDropdown()
        setupDropdown(binding.dropdownHotlistSourceType, HotlistSourceType.entries.toTypedArray()) {
            hotlistSourceType = it
        }
        binding.dropdownHotlistSourceType.setText(HotlistSourceType.PASSED_ONLY.name, false)

        setupDropdown(binding.dropdownResponseMode, ResponseMode.entries.toTypedArray()) {
            responseMode = it
        }
        binding.dropdownResponseMode.setText(ResponseMode.SINGLE.name, false)

        setupDropdown(binding.dropdownCameraType, CameraType.entries.toTypedArray()) {
            cameraType = it
        }
        binding.dropdownCameraType.setText(CameraType.DEVICE_CAMERA.name, false)

        setupDropdown(binding.dropdownResponseType, ResponseType.entries.toTypedArray()) {
            responseType = it
        }
        binding.dropdownResponseType.setText(ResponseType.ALL_ALERTS.name, false)
    }

    // Setup database dropdown and listeners
    private fun setupDatabaseDropdown() {
        val selectFileAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            listOf(
                getString(R.string.use_test_data_base),
                getString(R.string.select_from_device_storage)
            )
        )
        with(binding.dropdownDatabase) {
            setAdapter(selectFileAdapter)
            onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                if (position == 0) {
                    useTestDatabase()
                } else {
                    launchSelectDatabaseIntent()
                }
            }
        }
    }

    // Generalized dropdown setup function
    private fun <T> setupDropdown(
        dropdown: AutoCompleteTextView,
        items: Array<T>,
        onSelect: (T) -> Unit
    ) {
        dropdown.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                items
            )
        )
        dropdown.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            onSelect(items[position])
        }
    }

    // Use the test database from assets
    private fun useTestDatabase() {
        val testDatabaseFile = viewModel.copyDatabaseFromAssets()
        testDatabaseFile?.let { file ->
            hotlistDatabaseFile = file
            hotlistDatabaseUri =
                FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
            table = "plates_table"
            binding.dropdownDatabase.setText(file.path, false)
            binding.dropdownAddTables.setText("plates_table", false)
        }
    }

    // Handle the result when a database is selected from device storage
    private fun handleDatabaseSelectionResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                hotlistDatabaseFile = viewModel.copyDatabaseToInternalStorage(uri)
                hotlistDatabaseFile?.let { file ->
                    hotlistDatabaseUri = FileProvider.getUriForFile(
                        this,
                        "${BuildConfig.APPLICATION_ID}.fileprovider",
                        file
                    )
                    table = null
                    populateTablesDropdown(file)
                }
            }
        }
    }

    // Populate table names in the dropdown based on selected database
    private fun populateTablesDropdown(databaseFile: File) {
        val tables = viewModel.getAllTablesAndColumns(databaseFile)
        val tablesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, tables)
        binding.dropdownAddTables.setAdapter(tablesAdapter)
        binding.dropdownAddTables.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                table = tables[position]
            }
        binding.dropdownDatabase.setText(databaseFile.path, false)
        binding.dropdownAddTables.setText(null, false)
    }

    // Launch the ALPR Scanner with the selected configuration
    private fun launchALPRScanner() {
        binding.tvLogs.text = ""
        logBuilder.clear()
        alprLauncherBuilder.clearAll()

        // Ensure all necessary data is present
        if (hotlistDatabaseFile == null || hotlistDatabaseUri == null) {
            printLogs(getString(R.string.database_file_and_uri_can_t_be_null))
            return
        }

        if (table.isNullOrEmpty()) {
            printLogs(getString(R.string.target_table_can_t_be_null_or_empty))
            return
        }

        // Configure and start ALPR launcher
        alprLauncherBuilder
            .setCameraType(cameraType)
            .setResponseType(responseType)
            .setHotlistFileAndUri(hotlistDatabaseFile!!, hotlistDatabaseUri!!)
            .addHotlist(table!!)
            .setHotlistSourceType(hotlistSourceType)

        alprLauncher.start(this, alprLauncherBuilder)
    }

    // Launch intent to select a database from device storage
    private fun launchSelectDatabaseIntent() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        selectDatabaseLauncher.launch(intent)
    }

    // Log messages
    private fun printLogs(data: String) {
        logBuilder.append(data).append("\n\n\n")
        lifecycleScope.launch(Dispatchers.Main) {
            binding.tvLogs.text = logBuilder.toString()
            binding.svLogs.post { binding.svLogs.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    // Handle ALPR launcher callbacks
    override fun onFailure(throwable: Throwable) {
        printLogs(getString(R.string.error_with_message, throwable.message.toString()))
    }

    override fun onSuccess(result: String) {
        printLogs(result)
        if (responseMode == ResponseMode.SINGLE) {
            alprLauncher.stop(this)
        }
    }

    override fun onCancelled() {
        printLogs(getString(R.string.cancelled_by_user))
    }

    override fun onDestroy() {
        alprLauncher.destroyLauncher(this)
        super.onDestroy()
    }
}