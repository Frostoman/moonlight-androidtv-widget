package com.androidtv.gameswidget

import android.app.Application
import com.androidtv.gameswidget.crypto.CryptoProvider
import com.androidtv.gameswidget.data.HostStore
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class App : Application() {

    lateinit var crypto: CryptoProvider
        private set
    lateinit var hostStore: HostStore
        private set

    override fun onCreate() {
        super.onCreate()
        // Ensure BouncyCastle is available for certificate generation/signing.
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())

        crypto = CryptoProvider(this)
        hostStore = HostStore(this)
    }

    companion object {
        fun from(ctx: android.content.Context) = ctx.applicationContext as App
    }
}
