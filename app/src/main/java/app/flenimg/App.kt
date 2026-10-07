package app.flenimg
import android.app.Application
class App : Application() {
    override fun onCreate() { super.onCreate(); AppHolder.context = applicationContext }
}
