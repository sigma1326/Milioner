package com.ads.milioner.View

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.ads.milioner.Model.AppManager
import com.ads.milioner.Model.database.DataBaseRepositoryImpl
import com.ads.milioner.R
import com.ads.milioner.ViewModel.GameViewModel
import com.ads.milioner.game.Tile
import kotlinx.android.synthetic.main.game_fragment.*
import org.koin.android.ext.android.inject

class GameFragment : Fragment() {

    private lateinit var viewModel: GameViewModel

    private val TAG = AppManager.TAG


    private val db: DataBaseRepositoryImpl by inject()
    private val WIDTH = "width"
    private val HEIGHT = "height"
    private val SCORE = "score"
    private val HIGH_SCORE = "high score temp"
    private val UNDO_SCORE = "undo score"
    private val CAN_UNDO = "can undo"
    private val UNDO_GRID = "undo"
    private val GAME_STATE = "game state"
    private val UNDO_GAME_STATE = "undo game state"
    private val MAX_TILE = "max tile"
    private val TIMER = "timer"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.game_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(GameViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val settings = PreferenceManager.getDefaultSharedPreferences(activity)
        game_view?.hasSaveState = settings.getBoolean("save_state", false)

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("hasState")) {
                load()
            }
        }


        // Elapsed Time Counter
        val t = object : Thread() {

            override fun run() {
                try {
                    while (!isInterrupted) {
                        Thread.sleep(1000)
                        Handler(Looper.getMainLooper()).post {
                            if (AppManager.running) {
                                game_view?.setElTime()
                            }
                        }
                    }
                } catch (ignored: InterruptedException) {
                    ignored.printStackTrace()
                }

            }
        }
        t.start()

        load()
        game_view?.game?.newGame()

        game_view?.setOnGameEndedListener {
        }
    }


    override fun onPause() {
        super.onPause()
        save()
        AppManager.running = false
    }

    override fun onResume() {
        super.onResume()
        AppManager.running = true
    }


    override fun onStop() {
        super.onStop()
        save()
        AppManager.running = false
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("hasState", true)
        save()
    }


    private fun save() {
        val settings = PreferenceManager.getDefaultSharedPreferences(activity)
        val editor = settings.edit()
        val field = game_view?.game?.grid?.field
        val undoField = game_view?.game?.grid?.undoField
        editor.putInt(WIDTH, field?.size!!)
        editor.putInt(HEIGHT, field.size)
        for (xx in field.indices) {
            for (yy in 0 until field[0]?.size!!) {
                if (field[xx][yy] != null) {
                    editor.putInt("$xx $yy", field[xx][yy].value)
                } else {
                    editor.putInt("$xx $yy", 0)
                }

                if (undoField!![xx][yy] != null) {
                    editor.putInt("$UNDO_GRID$xx $yy", undoField[xx][yy].value)
                } else {
                    editor.putInt("$UNDO_GRID$xx $yy", 0)
                }
            }
        }
        editor.putLong(SCORE, game_view?.game?.score!!)
        editor.putLong(HIGH_SCORE, game_view?.game?.highScore!!)
        editor.putLong(UNDO_SCORE, game_view?.game?.lastScore!!)
        editor.putBoolean(CAN_UNDO, game_view?.game?.canUndo!!)
        editor.putInt(GAME_STATE, game_view?.game?.gameState!!)
        editor.putInt(UNDO_GAME_STATE, game_view?.game?.lastGameState!!)
        editor.putLong(MAX_TILE, game_view?.game?.maxTile!!)
        editor.putInt(TIMER, game_view?.elTime!!)
        editor.apply()
    }


    private fun load() {
        //Stopping all animations
        game_view?.game?.aGrid?.cancelAnimations()

        val settings = PreferenceManager.getDefaultSharedPreferences(activity)
        for (xx in game_view?.game?.grid?.field?.indices!!) {
            for (yy in 0 until game_view?.game?.grid?.field!![0].size) {
                val value = settings.getInt("$xx $yy", -1)
                if (value > 0) {
                    game_view?.game?.grid?.field!![xx][yy] = Tile(xx, yy, value)
                } else if (value == 0) {
                    game_view?.game?.grid?.field!![xx][yy] = null
                }

                val undoValue = settings.getInt("$UNDO_GRID$xx $yy", -1)
                if (undoValue > 0) {
                    game_view?.game?.grid!!.undoField[xx][yy] = Tile(xx, yy, undoValue)
                } else if (value == 0) {
                    game_view?.game?.grid!!.undoField[xx][yy] = null
                }
            }
        }

//        game_view?.game?.score = settings.getLong(SCORE, game_view?.game?.score!!)
        game_view?.game?.highScore = settings.getLong(HIGH_SCORE, game_view?.game?.highScore!!)
        game_view?.game?.lastScore = settings.getLong(UNDO_SCORE, game_view?.game?.lastScore!!)
//        game_view?.game?.canUndo = settings.getBoolean(CAN_UNDO, game_view?.game?.canUndo!!)
//        game_view?.game?.gameState = settings.getInt(GAME_STATE, game_view?.game?.gameState!!)
//        game_view?.game?.lastGameState = settings.getInt(UNDO_GAME_STATE, game_view?.game?.lastGameState!!)
//        game_view?.game?.maxTile = settings.getLong(MAX_TILE, game_view?.game?.maxTile!!)
//        game_view?.elTime = settings.getInt(TIMER, game_view?.elTime!!)
    }

}
