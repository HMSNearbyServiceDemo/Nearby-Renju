package win.regin.renju

import android.Manifest
import android.graphics.ColorMatrixColorFilter
import android.graphics.Point
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.widget.toast
import com.huawei.hms.nearby.Nearby
import com.huawei.hms.nearby.StatusCode
import com.huawei.hms.nearby.discovery.*
import com.huawei.hms.nearby.transfer.Data
import com.huawei.hms.nearby.transfer.DataCallback
import com.huawei.hms.nearby.transfer.TransferEngine
import com.huawei.hms.nearby.transfer.TransferStateUpdate
import kotlinx.android.synthetic.main.activity_main.*

import java.util.*
import android.support.v4.app.SupportActivity
import android.support.v4.app.SupportActivity.ExtraData
import android.support.v4.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.R.attr.name
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Handler
import android.os.Message
import android.support.v7.app.AlertDialog


/**
 * @author :Reginer in  2018/7/9 21:36.
 *         联系方式:QQ:282921012
 *         功能描述:
 */
class MainActivity : AppCompatActivity(), RenjuCallback, View.OnClickListener {

    /**
     * 引擎
     */
    private var isYourTurn : Int = 0
    private lateinit var mAi: Ai
    private var mChooseChess: PopupWindow? = null
    private var mTransferEngine: TransferEngine? = null
    private var mDiscoveryEngine: DiscoveryEngine? = null
    private var mRemoteEndpointId: String? = null
    private var myNameStr: String? = null
    private var opNameStr: String? = null
    private var rank:Int = 0
    private var mIsBlack:Boolean = false
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    private val REQUEST_CODE_REQUIRED_PERMISSIONS = 1
    private var lastMove: Point = Point()
    private var connectTaskResult: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS)
        setContentView(R.layout.activity_main)
        mAi = Ai(this)
        rvRenju.setCallBack(this)
        rvRenju.viewTreeObserver.addOnGlobalLayoutListener { initPop(getWidth(), getHeight()) }
        val levelPosition = SpUtils[RenjuConstant.RENJU_LEVEL, 0] as Int
        level.text = resources.getStringArray(R.array.renju_level)[levelPosition]
        mAi.setLevel(levelPosition)

        mTransferEngine = Nearby.getTransferEngine(getApplicationContext())
        mDiscoveryEngine =  Nearby.getDiscoveryEngine(getApplicationContext() )
        connectTaskResult = StatusCode.STATUS_ENDPOINT_UNKNOWN

        val preferences = getSharedPreferences("usrInfo", Context.MODE_PRIVATE)

        rank = preferences.getInt("rank" ,1)
        myNameStr = preferences.getString("Nickname", "UnknownPlayer") + " " + rank + "段"

        newRenju.setOnClickListener(this)
        undo.setOnClickListener(this)
        level.setOnClickListener(this)
        suggest.setOnClickListener(this)
        mode.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        if (mAi.isAiThing) {
            toast(R.string.ai_thing)
            return
        }
        when (view) {
            newRenju -> {
                if (rvRenju.isHumanComputer) {
                    mChooseChess?.showAtLocation(rvRenju, Gravity.CENTER, 0, 0)
                }
                mAi.restart()
                rvRenju.start()
            }
            undo -> {
                if (rvRenju.chessCount >= 2) {
                    mAi.undo()
                    rvRenju.undo()
                }
            }
            level -> {
                var levelPosition = SpUtils[RenjuConstant.RENJU_LEVEL, 0] as Int
                levelPosition = if (levelPosition < 4) levelPosition + 1 else 0
                level.text = resources.getStringArray(R.array.renju_level)[levelPosition]
                SpUtils.put(RenjuConstant.RENJU_LEVEL,levelPosition)
                mAi.setLevel(levelPosition)
            }
            suggest -> {
//                if (!rvRenju.isGameOver) {
//                    rvRenju.setUserBout(false)
//                    launch {
//                        val suggestPoint = mAi.suggest()
//                        launch(UI) {
//                            rvRenju.setUserBout(true)
//                            rvRenju.showSuggest(suggestPoint)
//                        }
//                    }
//                }
                this.onReveiveAction(Point(Random().nextInt(14), Random().nextInt(14)))
            }
            mode -> {

                if (TextUtils.equals(mode.text.toString(), getString(R.string.human_human))) {
                    mode.text = getString(R.string.human_computer)
                    rvRenju.gameMode = RenjuConstant.HUMAN_COMPUTER
                    mAi.aiChess = rvRenju.userChess
                    rvRenju.userChess = mAi.getUserChess()
                    rvRenju.setUserBout(false)
                    aiThink(null)
                } else {
                    mode.text = getString(R.string.human_human)
                    rvRenju.gameMode = RenjuConstant.HUMAN_HUMAN
                }



            }
        }
    }

    /**
     * 初始化PopWindow
     *
     * @param width  宽度
     * @param height 高度
     */
    private fun initGameOverPop(width: Int, height: Int, isBlack:Boolean,  isWinner:Boolean) {

        val builder = AlertDialog.Builder(this)
        val preferences = getSharedPreferences("usrInfo", Context.MODE_PRIVATE)
        rank = preferences.getInt("rank" ,1)
        var rankAfter:Int = rank
        if (isWinner)
        {
            if (rank < 9)
            {
                rankAfter = rank + 1;
            }
        }
        else
        {
            if (rank > 1)
            {
                rankAfter = rank - 1
            }
        }
        val editor = preferences.edit()
        editor.putInt("rank", rankAfter)
        editor.apply()
        builder.setTitle("游戏结束")//设置弹出对话框的标题

        var msg:String
        if (isWinner)
            msg = "胜不骄\n"//设置弹出对话框的内容
        else
            msg = "败不馁\n"
        msg += "段位："+rank+"段 -> "+rankAfter+"段"
        builder.setMessage(msg)
        builder.setCancelable(false)//能否被取消
        //正面的按钮（肯定）
        builder.setPositiveButton("返回主菜单", object : DialogInterface.OnClickListener {

            override fun onClick(dialog: DialogInterface, which: Int) {
                mDiscoveryEngine?.disconnectAll();
                finish();

            }
        })
        builder.show()
    }

    private fun aiThink(p: Point?) {
        mAi.aiBout(p)
    }

    private fun initPop(width: Int, height: Int) {
        if (mChooseChess == null) {
            val view = View.inflate(this, R.layout.view_pop_choose_chess, null)
            val white = view.findViewById<ImageButton>(R.id.choose_white)
            val black = view.findViewById<ImageButton>(R.id.choose_black)
            val text = view.findViewById<TextView>(R.id.textView6)
            white.setOnClickListener {
                text.setText(" 您已选择执白\n正在寻找对手...")
                rvRenju.setUserBout(false)
                black.setEnabled(false)
                black.setVisibility(View.INVISIBLE)
                rvRenju.userChess = RenjuConstant.WHITE_CHESS
                mAi.aiChess = RenjuConstant.BLACK_CHESS
                //aiThink(null)
                isYourTurn = 0

                mIsBlack = false;
                val discBuilder = ScanOption.Builder()
                discBuilder.setPolicy(Policy.POLICY_STAR)
                mDiscoveryEngine?.startScan("nearbyRenju", mDiscCb, discBuilder.build())
                handler.sendEmptyMessageDelayed(0, 20000)

            }
            black.setOnClickListener {
                text.setText(" 您已选择执黑\n正在寻找对手...")
                rvRenju.setUserBout(true)
                white.setEnabled(false)
                white.setVisibility(View.INVISIBLE)
                rvRenju.userChess = RenjuConstant.BLACK_CHESS
                mAi.aiChess = RenjuConstant.WHITE_CHESS
                isYourTurn = 1;

                val advBuilder = BroadcastOption.Builder()
                advBuilder.setPolicy(Policy.POLICY_STAR)
                mIsBlack = true;
                Log.d("renju", "Broadcast $myNameStr")
                mDiscoveryEngine?.startBroadcasting(myNameStr, "nearbyRenju", mConnCb, advBuilder.build())
                handler.sendEmptyMessageDelayed(0, 20000)
            }
            mChooseChess = PopupWindow(view, width, height)
            mChooseChess?.isOutsideTouchable = false
            mChooseChess?.showAtLocation(rvRenju, Gravity.CENTER, 0, 0)
        }
    }

    override fun gameOver(winner: Int) {
        SoundPlayUtils.play(HintConstant.GAME_OVER)

                when (winner) {
                    RenjuConstant.BLACK_CHESS ->
                    {
                        initGameOverPop(getWidth(), getHeight(),true,true)
                    }
                    RenjuConstant.WHITE_CHESS ->
                    {
                        initGameOverPop(getWidth(), getHeight(),false, true)
                    }
                    else -> R.string.no_win
                }

        var p:Point = Point();
        when (winner) {
            RenjuConstant.BLACK_CHESS -> p.x = -1
            RenjuConstant.WHITE_CHESS -> p.x = -2
            else -> p.x = -3
        }
        p.y = 0;
        send(p)
    }

    override fun atBell(p0: Point, isAi: Boolean, isBlack: Boolean) {

        SoundPlayUtils.play(HintConstant.GAME_MOVE)
        Log.d("renju", "atBell isAi:" + isAi)
        if (isAi) aiAtBell(p0) else userAtBell(p0)
    }

    /**
     * ai落子
     */
    private fun aiAtBell(p: Point?) {
        Log.d("renju", "isYourTurn" + isYourTurn)

        if (p != null) {
            if (p.x <0 || p.x >= 15 || p.y < 0 || p.y >= 15) {
                return
            }
        }
        rvRenju.addChess(p, mAi.aiChess)
        rvRenju.setUserBout(true)
        rvRenju.checkGameOver()
    }

    /**
     * 玩家落子
     */
    private fun userAtBell(p: Point) {
        if (isYourTurn == 0)
            return
        send(p)
        if (rvRenju.isHumanComputer)
            //aiThink(p)
        else {
            if (p != null) {
                if (p.x < 0 || p.x >= 15 || p.y < 0 || p.y >= 15) {
                    return
                }
            }
            mAi.addChess(p)
        }
        //send to other

        isYourTurn = 0
    }


    private fun onReveiveAction(p : Point) {
        Log.d("renju", "isYourTurn" + isYourTurn)
        if (isYourTurn == 1)
            return
        if (p != null) {
            if (p.x <0 || p.x >= 15 || p.y < 0 || p.y >= 15) {
                return
            }
        }
        rvRenju.addChess(p, mAi.aiChess)
        rvRenju.setUserBout(true)
        rvRenju.checkGameOver()
        isYourTurn = 1
    }

    private val mDiscCb = object : ScanEndpointCallback() {
        override fun onFound(endpointId: String, discoveryEndpointInfo: ScanEndpointInfo) {
            mRemoteEndpointId = endpointId
            mDiscoveryEngine?.requestConnect(myNameStr, mRemoteEndpointId, mConnCb)
            Log.d("renju", "Nearby Client found Server and request connection. Server id:$endpointId")
        }

        override fun onLost(endpointId: String) {
            Log.d("renju", "Nearby Lost endpoint: $endpointId")
        }
    }

    private val mConnCb = object : ConnectCallback() {
        override fun onEstablish(endpointId: String, connectionInfo: ConnectInfo) {
            mDiscoveryEngine?.acceptConnect(endpointId, mDataCb)
            Log.d("renju", "Nearby Client accept connection from:$endpointId")
            Log.d("renju", "Scanner " + connectionInfo.endpointName)
            opNameStr = connectionInfo.endpointName
            if (mIsBlack)
            {
                blackName.setText(myNameStr)
                whiteName.setText(opNameStr)
            }
            else
            {
                blackName.setText(opNameStr)
                whiteName.setText(myNameStr)
            }
        }

        override fun onResult(endpointId: String, result: ConnectResult) {
            when (result.status.statusCode) {
                StatusCode.STATUS_SUCCESS -> {
                    mDiscoveryEngine?.stopScan()
                    mDiscoveryEngine?.stopBroadcasting()
                    connectTaskResult = StatusCode.STATUS_SUCCESS
                    SoundPlayUtils.play(HintConstant.GAME_START)
                    mChooseChess?.dismiss()
                }
                StatusCode.STATUS_CONNECT_REJECTED -> {
                }
            }/* The Connection was rejected. *//* other unknown status code. */
            mRemoteEndpointId = endpointId
        }

        override fun onDisconnected(endpointId: String) {
            Log.d("renju", "Nearby Client Disconnected from:$endpointId")
            connectTaskResult = StatusCode.STATUS_NOT_CONNECTED
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("断开连接")//设置弹出对话框的标题
            builder.setMessage("对方已退出对局，请返回")//设置弹出对话框的内容
            builder.setCancelable(false)//能否被取消
            //正面的按钮（肯定）
            builder.setPositiveButton("返回主菜单", object : DialogInterface.OnClickListener {

                override fun onClick(dialog: DialogInterface, which: Int) {
                    //mDiscoveryEngine?.disconnectAll();
                    finish();

                }
            })
            builder.show()
        }
    }

    private val mDataCb = object : DataCallback() {
        override fun onReceived(endpointId: String, data: Data) {
            //receiveMessage(data);
            Log.d("renju", "Client received data. from : " + endpointId + " Data id:" + data.id)
            var value: Int
            var p:Point = Point()
            val src = data.asBytes()
            value = (src[0].toInt() and 0xFF
                    or (src[1].toInt() and 0xFF shl 8)
                    or (src[2].toInt() and 0xFF shl 16)
                    or (src[3].toInt() and 0xFF shl 24))
            p.x = value;
            value = (src[4].toInt() and 0xFF
                    or (src[5].toInt() and 0xFF shl 8)
                    or (src[6].toInt() and 0xFF shl 16)
                    or (src[7].toInt() and 0xFF shl 24))
            p.y = value;

            if (p.x < 0)
            {
                        when (p.x) {
                            -1 ->
                            {
                                initGameOverPop(getHeight(),getWidth(),true, false)
                            }
                            -2 ->
                            {
                                initGameOverPop(getHeight(),getWidth(),false, false)
                            }
                        }
                return
            }

            onReveiveAction(p)
        }

        override fun onTransferUpdate(endPointId: String, update: TransferStateUpdate) {

        }
    }

    private fun send(p:Point) {
        lastMove = p
        val src = ByteArray(8)
        src[3] = (p.x shr 24 and 0xFF).toByte()
        src[2] = (p.x shr 16 and 0xFF).toByte()
        src[1] = (p.x shr 8 and 0xFF).toByte()
        src[0] = (p.x and 0xFF) .toByte()

        src[7] = (p.y shr 24 and 0xFF) .toByte()
        src[6] = (p.y shr 16 and 0xFF) .toByte()
        src[5] = (p.y shr 8 and 0xFF) .toByte()
        src[4] = (p.y and 0xFF).toByte()

        val data = Data.fromBytes(src)
        Log.d("renju", "Client sent data.to id:" + mRemoteEndpointId + " Data id: " + data.id)
        mTransferEngine?.sendData(mRemoteEndpointId, data)
    }

    private fun reSend()
    {
        send(lastMove)
    }

    override fun onStop() {
        super.onStop()
        Log.d("renju", "Onstop")
    }

    override fun onDestroy() {
        super.onDestroy()
        mDiscoveryEngine?.disconnectAll();
        mDiscoveryEngine?.stopBroadcasting()
        mDiscoveryEngine?.stopScan()
        Log.d("renju", "onDestroy")
    }

    override fun onPause() {
        super.onPause()
        Log.d("renju", "onPause")
    }

    /**
     * Handle timeout function
     */
    @SuppressLint("HandlerLeak")
    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            this.removeMessages(0)
            if (connectTaskResult == StatusCode.STATUS_ENDPOINT_UNKNOWN) {
                mDiscoveryEngine?.stopScan()
                mDiscoveryEngine?.stopBroadcasting()
                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle("连接超时")//设置弹出对话框的标题
                builder.setMessage("1.请线下确认对手是否选择了与您相反颜色的棋子。\n" +
                        "2.请确认HMS CORE和Nearby五子棋APP的位置权限已经打开。\n" +
                        "3.若以上2点确认无误，关闭再打开手机蓝牙开关，然后重新尝试。\n"+
                "4.连接建立有几率失败，在mate20上测试成功率为92%，低版本手机可能会更低。")//设置弹出对话框的内容
                builder.setCancelable(false)//能否被取消
                //正面的按钮（肯定）
                builder.setPositiveButton("返回主菜单", object : DialogInterface.OnClickListener {

                    override fun onClick(dialog: DialogInterface, which: Int) {
                        //mDiscoveryEngine?.disconnectAll();
                        finish();

                    }
                })
                builder.show()
            }
        }
    }
}

