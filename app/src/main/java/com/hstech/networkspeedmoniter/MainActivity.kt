package com.hstech.networkspeedmoniter

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.net.TrafficStats
import android.os.AsyncTask
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.row_item.view.*
import kotlinx.android.synthetic.main.row_item.view.description
import kotlinx.android.synthetic.main.row_item.view.switch1
import java.lang.ref.WeakReference
import java.net.URL

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: MyAdapter
    private lateinit var headings: Array<String>
    private lateinit var description: Array<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.statusBarColor = getColor(R.color.colorPrimaryDark)

        DownloadSpeedTest(WeakReference(this)).execute()

        imageButton.setOnClickListener {
            DownloadSpeedTest(WeakReference(this)).execute()
        }

        val sharedPreferences = getSharedPreferences("speedUnit", Context.MODE_PRIVATE)
        val s1 = sharedPreferences.getString("1", getString(R.string.Bps))
        val s2 = sharedPreferences.getString("2", getString(R.string.bps))
        headings = arrayOf("Enable download speed meter", "Speed meter unit", "Download speed test unit")
        description = arrayOf("Show download speed in status bar", s1!!, s2!!)
        MobileAds.initialize(this, "ca-app-pub-2945120579589145~9972094370")
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        adapter = MyAdapter(this, headings, description)
        list.adapter = adapter
        registerForContextMenu(list)
        list.setOnItemClickListener { _, view, _, _ ->
            openContextMenu(view)
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.unit_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {

        val sharedPreferences = getSharedPreferences("speedUnit", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val index = info.position
        when (item.itemId)
        {
            R.id.Bps -> {

                if (description[index] != getString(R.string.Bps))
                {
                    editor.putString(index.toString(), getString(R.string.Bps))
                    description[index] = getString(R.string.Bps)
                    adapter.notifyDataSetChanged()
                    refresh(index)
                }
            }
            R.id.bps -> {
                if (description[index] != getString(R.string.bps))
                {
                    editor.putString(index.toString(), getString(R.string.bps))
                    description[index] = getString(R.string.bps)
                    adapter.notifyDataSetChanged()
                    refresh(index)
                }
            }
        }
        editor.apply()
        return false
    }

    private fun refresh(x: Int)
    {
        if (x == 1)
        {
            val sharedPref = getSharedPreferences("serviceCheck", Context.MODE_PRIVATE)
            if (sharedPref.getBoolean("serviceRunning", false))
            {
                stopService(Intent(this, InternetSpeedMeter::class.java))
                startService(Intent(this, InternetSpeedMeter::class.java))
            }
        }
        else if (x == 2)
            DownloadSpeedTest(WeakReference(this)).execute()
    }
    private class DownloadSpeedTest(private val contextReference: WeakReference<Context>) : AsyncTask<Void, Void, Int>() {
        override fun onPostExecute(result: Int?) {
            val activity = contextReference.get() as MainActivity
            activity.textView5.visibility = View.VISIBLE
            activity.imageButton.visibility = View.VISIBLE
            activity.progressBar.visibility = View.INVISIBLE
            activity.textView3.text = activity.getString(R.string.your_download_speed_is)
            var speed = ""
            if (result != null) {
                val context = contextReference.get()
                val sharedPreferences = context?.getSharedPreferences("speedUnit", Context.MODE_PRIVATE)
                val str = sharedPreferences?.getString("2", context.getString(R.string.bps))

                if (str == context?.getString(R.string.Bps))
                {
                    speed = when {
                        result < 0 -> "0 KB/s"
                        result in 0..999 -> "$result KB/s"
                        else -> String.format("%.2f", result / 1024f) + " MB/s"
                    }
                }

                else if (str == context?.getString(R.string.bps))
                {
                    var r = result
                    r *= 8
                    speed = when {
                        r < 0 -> "0 Kb/s"
                        r in 0..999 -> "$r Kb/s"
                        else -> String.format("%.2f", r / 1024f) + " Mb/s"
                    }
                }
            }
            activity.textView5.text = speed
        }

        override fun onPreExecute() {
            val activity = contextReference.get() as MainActivity
            activity.textView5.visibility = View.INVISIBLE
            activity.imageButton.visibility = View.INVISIBLE
            activity.progressBar.visibility = View.VISIBLE
            activity.textView3.text = activity.getString(R.string.testing)
        }
        override fun doInBackground(vararg p0: Void?): Int {
            try {
                val inputStream =
                    URL("https://dl.google.com/dl/android/studio/install/3.4.1.0/android-studio-ide-183.5522156-windows.exe").openStream()
                val buf = ByteArray(5000)
                var startBytes: Long = 0
                var startTime: Long = 0
                for (i in 0..500) {
                    inputStream.read(buf)
                    if (i == 200)
                    {
                        startBytes = TrafficStats.getTotalRxBytes()
                        startTime = System.nanoTime()
                    }
                }
                val endTime = System.nanoTime()
                val endBytes = TrafficStats.getTotalRxBytes()
                val totalTimeInSec = (endTime - startTime) / 1000000000.0
                val totalDataInKB = (endBytes - startBytes) / 1024.0
                val speedDecimal = totalDataInKB / totalTimeInSec
                return speedDecimal.toInt()
            } catch (e: Exception) {
                e.printStackTrace()
                return -1
            }
        }
    }

    class MyAdapter(context: Context, private val headings: Array<String>, private val description: Array<String>) : ArrayAdapter<String>(context, 0, headings) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView
            if (view == null)
                view = LayoutInflater.from(context).inflate(R.layout.row_item, parent, false)
            if (position == 0) {
                view?.switch1?.visibility = View.VISIBLE
                val sharedPreferences = context.getSharedPreferences("serviceCheck", Context.MODE_PRIVATE)
                if (sharedPreferences.getBoolean("serviceRunning", false))
                    view?.switch1?.isChecked = true
                view?.switch1?.setOnCheckedChangeListener { _, b ->
                    val intent = Intent(context, InternetSpeedMeter::class.java)
                    if (b)
                    {
                        context.startService(intent)
                        saveServiceStatus(true)
                    }
                    if (!b)
                    {
                        context.stopService(intent)
                        saveServiceStatus(false)
                    }
                }
            }
            view?.heading?.text = headings[position]
            view?.description?.text = description[position]
            return view!!
        }

        private fun saveServiceStatus(x: Boolean) {
            val sharedPreferences = context.getSharedPreferences("serviceCheck", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("serviceRunning", x)
            editor.apply()
        }
    }
}