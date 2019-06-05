package com.xxss0903.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initProgressBar()
    }

    private fun initProgressBar() {
        btn_start.setOnClickListener {
            //
            progress1Start()
            progress2.progressTotal = 3000
            progress2.startByCoroutines()
            progress3.startByCoroutines()
            progress4.startByCoroutines()
            progress5.startByCoroutines()
        }
        btn_reset.setOnClickListener {
            progress2.cancelCoroutine()
            progress3.cancelCoroutine()
            progress4.cancelCoroutine()
            progress5.cancelCoroutine()

            progress2.resetProgress()
            progress3.resetProgress()
            progress4.resetProgress()
            progress5.resetProgress()

            progress1.cancelCoroutine()
        }
    }

    private fun progress1Start() {
        progress1.progressTotal = 12000
        GlobalScope.launch {
            progress1.progress = 10f
            delay(100)
            progress1.progress = 100f
            delay(100)
            progress1.progress = 1000f
            delay(100)
            progress1.progress = 2000f
            delay(100)
            progress1.progress = 2400f
            delay(100)
            progress1.progress = 3600f
            delay(100)
            progress1.progress = 4600f
            delay(100)
            progress1.progress = 5200f
            delay(100)
            progress1.progress = 5600f
            delay(100)
            progress1.progress = 5900f
            delay(100)
            progress1.progress = 6900f
            delay(100)
            progress1.progress = 9900f
            delay(100)
            progress1.progress = 12000f
            delay(100)
        }
    }
}
