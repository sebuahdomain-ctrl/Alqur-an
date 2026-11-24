package com.sabilu.alquran

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ReaderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        findViewById<TextView>(R.id.title).text = intent.getStringExtra(EXTRA_TITLE)
        findViewById<android.view.View>(R.id.back).setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_TITLE = "title"
    }
}
