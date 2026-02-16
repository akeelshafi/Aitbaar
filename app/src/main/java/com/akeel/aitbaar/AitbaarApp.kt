package com.akeel.aitbaar

import android.app.Application
import com.akeel.aitbaar.data.repository.TransactionRepository

class AitbaarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TransactionRepository.init(this)
    }
}
