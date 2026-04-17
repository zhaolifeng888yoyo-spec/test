package com.networkmonitor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.telephony.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var tv4gInfo: TextView
    private lateinit var tv5gInfo: TextView
    private lateinit var tvSignalInfo: TextView
    private lateinit var chartRsrp: LineChart
    private lateinit var btnExport: Button

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 1000L
    private val rsrpEntries = mutableListOf<Entry>()
    private val dataRecords = mutableListOf<String>()
    private var timeCounter = 0f

    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        tv4gInfo = findViewById(R.id.tv4gInfo)
        tv5gInfo = findViewById(R.id.tv5gInfo)
        tvSignalInfo = findViewById(R.id.tvSignalInfo)
        chartRsrp = findViewById(R.id.chartRsrp)
        btnExport = findViewById(R.id.btnExport)

        setupChart()
        checkPermissions()

        btnExport.setOnClickListener {
            exportToCSV()
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            startMonitoring()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startMonitoring()
            } else {
                Toast.makeText(this, "需要权限才能监测网络", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupChart() {
        chartRsrp.description.isEnabled = false
        chartRsrp.setTouchEnabled(false)
        chartRsrp.isDragEnabled = false
        chartRsrp.setScaleEnabled(false)
        chartRsrp.setPinchZoom(false)
        chartRsrp.xAxis.setDrawGridLines(false)
        chartRsrp.axisLeft.setDrawGridLines(true)
        chartRsrp.axisRight.isEnabled = false
        chartRsrp.legend.isEnabled = false
    }

    private fun startMonitoring() {
        handler.post(object : Runnable {
            override fun run() {
                updateNetworkInfo()
                handler.postDelayed(this, updateInterval)
            }
        })
    }

    private fun updateNetworkInfo() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val allCellInfo = telephonyManager.allCellInfo ?: return
        var rsrpValue = -140

        for (cellInfo in allCellInfo) {
            when (cellInfo) {
                is CellInfoLte -> {
                    val identity = cellInfo.cellIdentity as CellIdentityLte
                    val signal = cellInfo.cellSignalStrength as CellSignalStrengthLte

                    val mcc = if (identity.mccString != null) identity.mccString else "--"
                    val mnc = if (identity.mncString != null) identity.mncString else "--"
                    val tac = if (identity.tac != Int.MAX_VALUE) identity.tac.toString() else "--"
                    val ci = if (identity.ci != Int.MAX_VALUE) identity.ci.toString() else "--"
                    val pci = if (identity.pci != Int.MAX_VALUE) identity.pci.toString() else "--"
                    val earfcn = if (identity.earfcn != Int.MAX_VALUE) identity.earfcn.toString() else "--"

                    tv4gInfo.text = "MCC: $mcc | MNC: $mnc | TAC: $tac\nCI: $ci | PCI: $pci | EARFCN: $earfcn"

                    rsrpValue = signal.rsrp
                    val rsrq = signal.rsrq
                    val rssi = signal.rssi
                    val rssnr = signal.rssnr
                    val level = signal.level

                    tvSignalInfo.text = "RSRP: $rsrpValue dBm | RSRQ: $rsrq dB\nSINR: $rssnr dB | RSSI: $rssi dBm\n信号等级: $level"
                }
                is CellInfoNr -> {
                    val identity = cellInfo.cellIdentity as CellIdentityNr
                    val signal = cellInfo.cellSignalStrength as CellSignalStrengthNr

                    val mcc = if (identity.mccString != null) identity.mccString else "--"
                    val mnc = if (identity.mncString != null) identity.mncString else "--"
                    val tac = if (identity.tac != Int.MAX_VALUE) identity.tac.toString() else "--"
                    val nci = if (identity.nci != Long.MAX_VALUE) identity.nci.toString() else "--"
                    val pci = if (identity.pci != Int.MAX_VALUE) identity.pci.toString() else "--"
                    val nrarfcn = if (identity.nrarfcn != Int.MAX_VALUE) identity.nrarfcn.toString() else "--"

                    tv5gInfo.text = "MCC: $mcc | MNC: $mnc | TAC: $tac\nNCI: $nci | PCI: $pci | NR ARFCN: $nrarfcn"

                    rsrpValue = signal.ssRsrp
                    val rsrq = signal.ssRsrq
                    val sinr = signal.ssSinr
                    val level = signal.level

                    tvSignalInfo.text = "RSRP: $rsrpValue dBm | RSRQ: $rsrq dB\nSINR: $sinr dB | RSSI: -- dBm\n信号等级: $level"
                }
            }
        }

        updateChart(rsrpValue)
        recordData(rsrpValue)
    }

    private fun updateChart(rsrp: Int) {
        timeCounter++
        rsrpEntries.add(Entry(timeCounter, rsrp.toFloat()))

        if (rsrpEntries.size > 60) {
            rsrpEntries.removeAt(0)
        }

        val dataSet = LineDataSet(rsrpEntries, "RSRP")
        dataSet.color = android.graphics.Color.parseColor("#2196F3")
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.lineWidth = 2f

        chartRsrp.data = LineData(dataSet)
        chartRsrp.notifyDataSetChanged()
        chartRsrp.invalidate()
    }

    private fun recordData(rsrp: Int) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val record = "$timestamp,$rsrp"
        dataRecords.add(record)
    }

    private fun exportToCSV() {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "network_monitor_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            val file = File(downloadsDir, fileName)

            FileWriter(file).use { writer ->
                writer.append("时间,RSRP(dBm)\n")
                dataRecords.forEach { record ->
                    writer.append("$record\n")
                }
            }

            Toast.makeText(this, "数据已导出到: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
