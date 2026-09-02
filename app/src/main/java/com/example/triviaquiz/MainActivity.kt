package com.example.triviaquiz

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.triviaquiz.data.QuizPreferences
import com.example.triviaquiz.data.QuizRepository
import com.example.triviaquiz.databinding.ActivityMainBinding
import com.example.triviaquiz.model.Question
import com.example.triviaquiz.model.QuizState
import com.example.triviaquiz.network.NetworkResult
import com.example.triviaquiz.network.NetworkUtils
import com.example.triviaquiz.util.BookmarkHelper
import com.example.triviaquiz.util.DialogHelper
import com.example.triviaquiz.util.SoundManager
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private val repo = QuizRepository()
    private lateinit var prefs: QuizPreferences
    private lateinit var snd: SoundManager
    private lateinit var bm: BookmarkHelper

    private var selCat: Int? = null; private var selDiff: String? = null; private var selCnt = 10
    private val cn = listOf("Any Category","General Knowledge","Books","Film","Music","Television","Video Games","Science: Gadgets","Computers","Mathematics","Mythology","Sports","Geography","History","Politics","Art","Celebrities","Animals")
    private val ci_ = listOf(null,9,10,11,12,14,15,17,18,19,20,21,22,23,24,25,26,27)
    private val dn = listOf("Any Difficulty","Easy","Medium","Hard"); private val dv = listOf(null,"easy","medium","hard")

    private var qs = listOf<Question>(); private var ss = mutableListOf<QuizState>()
    private var idx = 0; private var strk = 0; private var bst = 0
    private var f50 = true; private var skp = true; private var addT = true
    private var tmr: CountDownTimer? = null; private var tl = 20000L; private var tmrOn = false
    private var fa = 0L; private var qOn = false; private var daily = false
    private var pqs = listOf<Question>(); private var pi = 0; private var pc = 0; private var pa = false

    override fun onCreate(s: Bundle?) {
        super.onCreate(s); b = ActivityMainBinding.inflate(layoutInflater); setContentView(b.root)
        ViewCompat.setOnApplyWindowInsetsListener(b.main) { v, i -> val sb = i.getInsets(WindowInsetsCompat.Type.systemBars()); v.setPadding(sb.left,sb.top,sb.right,sb.bottom); i }
        prefs = QuizPreferences(this); snd = SoundManager(this); bm = BookmarkHelper(prefs)
        setupH(); setupQ(); setupR(); setupS(); setupP()
        if(prefs.hasActiveQuizSession()) { val ss_ = prefs.getQuizSession()!!; DialogHelper.showResumeQuiz(this,ss_.category,ss_.difficulty,ss_.currentIndex,ss_.count, { resume(ss_) }, { prefs.clearQuizSession(); show("home") }) } else show("home")
    }

    private fun setupH() {
        b.categorySpinner.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cn)
        b.categorySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener { override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) { selCat = ci_[pos] }; override fun onNothingSelected(p: android.widget.AdapterView<*>?) {} }
        b.difficultySpinner.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, dn)
        b.difficultySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener { override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) { selDiff = dv[pos] }; override fun onNothingSelected(p: android.widget.AdapterView<*>?) {} }
        val cbs = listOf(b.countBtn5,b.countBtn10,b.countBtn15,b.countBtn20)
        listOf(5,10,15,20).forEachIndexed { i,c -> cbs[i].setOnClickListener { selCnt=c; sb(cbs,i) } }
        b.startQuizButton.setOnClickListener { daily=false; load() }
        b.homeDailyButton.setOnClickListener { if(prefs.isDailyChallengeCompletedToday()) Toast.makeText(this,"Score: ${prefs.getDailyScore()}/${prefs.getDailyTotal()}",Toast.LENGTH_LONG).show() else { daily=true; selCat=null; selDiff="medium"; selCnt=10; load() } }
        b.statisticsButton.setOnClickListener { show("statistics") }; b.bookmarksHomeButton.setOnClickListener { show("bookmarks") }
        b.historyButton.setOnClickListener { show("history") }; b.achievementsButton.setOnClickListener { show("achievements") }; b.settingsButton.setOnClickListener { show("settings") }
    }
    private fun sb(btns: List<com.google.android.material.button.MaterialButton>, s: Int) { btns.forEachIndexed { i,b -> if(i==s){b.setBackgroundColor(getColor(R.color.primary_pink));b.setTextColor(Color.WHITE)}else{b.setBackgroundColor(Color.TRANSPARENT);b.setTextColor(getColor(R.color.chocolate_brown))} } }
    private fun rh() { b.homeBestScore.text="${prefs.getBestScore()} / ${prefs.getBestTotal()}"; b.homeQuizzesPlayed.text="${prefs.getQuizzesPlayed()}"; b.homeLevel.text="LEVEL ${prefs.getLevel()}"; b.homeXP.text="${prefs.getXP()} / ${prefs.getXPForNextLevel()} XP"; b.homeXPBar.progress=prefs.getXPProgress()
        if(prefs.isDailyChallengeCompletedToday()){b.homeDailySubtitle.text="Completed ✓";b.homeDailyScore.text="Score: ${prefs.getDailyScore()} / ${prefs.getDailyTotal()}";b.homeDailyScore.visibility=View.VISIBLE;b.homeDailyButton.text="View Result"}else{b.homeDailySubtitle.text="10 Questions • Medium";b.homeDailyScore.visibility=View.GONE;b.homeDailyButton.text="Start Challenge"} }

    private fun setupQ() {
        listOf(b.answerButton1,b.answerButton2,b.answerButton3,b.answerButton4).forEachIndexed { i,bt -> bt.setOnClickListener { ans(i) } }
        b.previousButton.setOnClickListener { pv() }; b.nextButton.setOnClickListener { nx() }
        b.flagButton.setOnClickListener { ss[idx].isFlagged=!ss[idx].isFlagged; dq() }
        b.bookmarkButton.setOnClickListener { val q=qs[idx]; Toast.makeText(this,if(bm.toggleBookmark(q))"Bookmarked!"else"Removed",Toast.LENGTH_SHORT).show(); dq() }
        b.quizBackButton.setOnClickListener { lvd() }; b.lifelineFiftyFifty.setOnClickListener { use50() }; b.lifelineSkip.setOnClickListener { usk() }; b.lifelineAddTime.setOnClickListener { uat() }; b.navigatorButton.setOnClickListener { tnav() }
    }
    private fun setupR() { b.reviewButton.setOnClickListener { show("review") }; b.resultFlaggedButton.setOnClickListener { show("flagged") }; b.playAgainButton.setOnClickListener { daily=false; load() }; b.homeButton.setOnClickListener { kt(); qOn=false; prefs.clearQuizSession(); show("home") }; b.practiceWrongButton.setOnClickListener { startP() } }
    private fun setupS() {
        b.historyBackButton.setOnClickListener { show("home") }; b.clearHistoryButton.setOnClickListener { DialogHelper.showConfirm(this,"Clear History","Are you sure?"){prefs.clearHistory();fH()} }
        b.achievementsBackButton.setOnClickListener { show("home") }; b.statisticsBackButton.setOnClickListener { show("home") }; b.bookmarksBackButton.setOnClickListener { show("home") }
        b.clearBookmarksButton.setOnClickListener { DialogHelper.showConfirm(this,"Clear Bookmarks","Remove all?"){bm.clearBookmarks();fB()} }
        b.settingsBackButton.setOnClickListener { show("home") }; b.reviewBackButton.setOnClickListener { show("result") }; b.flaggedBackButton.setOnClickListener { show("quiz") }
        b.resetDataButton.setOnClickListener { DialogHelper.showConfirm(this,"Reset","Erase all?"){prefs.resetAll();Toast.makeText(this,"Cleared",Toast.LENGTH_SHORT).show()} }
        b.soundSwitch.isChecked=prefs.isSoundEnabled(); b.vibrationSwitch.isChecked=prefs.isVibrationEnabled(); b.timerSwitch.isChecked=prefs.isTimerEnabled()
        val dbs=listOf(b.durationBtn10,b.durationBtn15,b.durationBtn20,b.durationBtn30); val ds_=listOf(10,15,20,30); sd(dbs,ds_.indexOf(prefs.getTimerDuration()).coerceAtLeast(0))
        dbs.forEachIndexed { i,bt -> bt.setOnClickListener { prefs.setTimerDuration(ds_[i]); sd(dbs,i) } }
        b.soundSwitch.setOnCheckedChangeListener { _,c->prefs.setSoundEnabled(c) }; b.vibrationSwitch.setOnCheckedChangeListener { _,c->prefs.setVibrationEnabled(c) }
        b.timerSwitch.setOnCheckedChangeListener { _,c->prefs.setTimerEnabled(c);b.timerDurationCard.visibility=if(c)View.VISIBLE else View.GONE }; b.timerDurationCard.visibility=if(prefs.isTimerEnabled())View.VISIBLE else View.GONE
    }
    private fun sd(btns: List<com.google.android.material.button.MaterialButton>, s: Int) { btns.forEachIndexed { i,b -> if(i==s){b.setBackgroundColor(getColor(R.color.primary_pink));b.setTextColor(Color.WHITE)}else{b.setBackgroundColor(Color.TRANSPARENT);b.setTextColor(getColor(R.color.chocolate_brown))} } }
    private fun setupP() { b.practiceBackButton.setOnClickListener { show("result") } }

    private fun show(s: String) { listOf(b.homeGroup,b.loadingGroup,b.errorGroup,b.quizGroup,b.resultGroup,b.reviewGroup,b.historyGroup,b.achievementsGroup,b.settingsGroup,b.flaggedGroup,b.statisticsGroup,b.bookmarksGroup,b.practiceGroup).forEach{it.visibility=View.GONE}
        when(s){"home"->{b.homeGroup.visibility=View.VISIBLE;rh()}; "loading"->b.loadingGroup.visibility=View.VISIBLE; "error"->b.errorGroup.visibility=View.VISIBLE
        "quiz"->{b.quizGroup.visibility=View.VISIBLE;qOn=true;dq()}; "result"->{b.resultGroup.visibility=View.VISIBLE;dR()}; "review"->{b.reviewGroup.visibility=View.VISIBLE;fR()}
        "history"->{b.historyGroup.visibility=View.VISIBLE;fH()}; "achievements"->{b.achievementsGroup.visibility=View.VISIBLE;fA()}; "settings"->b.settingsGroup.visibility=View.VISIBLE
        "flagged"->{b.flaggedGroup.visibility=View.VISIBLE;fF()}; "statistics"->{b.statisticsGroup.visibility=View.VISIBLE;fS()}; "bookmarks"->{b.bookmarksGroup.visibility=View.VISIBLE;fB()}
        "practice"->{b.practiceGroup.visibility=View.VISIBLE;dP()}} }

    private fun load() { show("loading"); if(!NetworkUtils.isInternetAvailable(this)){b.errorMessage.text="No internet connection.";show("error");return}
        lifecycleScope.launch { when(val r=repo.fetchQuestions(selCnt,selCat,selDiff)){is NetworkResult.Success->{initQ(r.data);show("quiz")}; is NetworkResult.Error->{b.errorMessage.text=r.message;show("error")}} } }
    private fun initQ(q: List<Question>) { qs=q; idx=0; strk=0; bst=0; ss=q.mapIndexed{i,_->QuizState(i)}.toMutableList(); f50=true; skp=true; addT=true; tl=prefs.getTimerDuration()*1000L; fa=System.currentTimeMillis(); qOn=true }

    private fun resume(s: QuizPreferences.QuizSessionData) { try {
        val qa=JSONArray(s.questionsJson); qs=(0 until qa.length()).map{val o=qa.getJSONObject(it);val a=o.getJSONArray("answers");Question(o.getString("text"),o.getString("correctAnswer"),(0 until a.length()).map{a.getString(it)},o.optString("category",""),o.optString("difficulty",""))}
        val sa=JSONArray(s.statesJson); ss=(0 until sa.length()).map{val o=sa.getJSONObject(it);val e=o.optJSONArray("eliminatedAnswers");QuizState(it,o.optString("selectedAnswer",null),o.optBoolean("isAnswered"),o.optBoolean("isCorrect"),o.optBoolean("isWrong"),o.optBoolean("isSkipped"),o.optBoolean("isFlagged"),o.optBoolean("isBookmarked"),if(e!=null)(0 until e.length()).map{e.getString(it)}else emptyList(),o.optBoolean("isFiftyFiftyUsed"))}.toMutableList()
        idx=s.currentIndex; strk=s.streak; bst=s.bestStreak; f50=!s.fiftyFiftyUsed; skp=!s.skipUsed; addT=!s.addTimeUsed; tl=prefs.getTimerDuration()*1000L; qOn=true; show("quiz")
    } catch(_:Exception){prefs.clearQuizSession();show("home")} }
    private fun sv() { if(!qOn||qs.isEmpty())return; try {
        val qa=JSONArray(); qs.forEach{q->qa.put(JSONObject().apply{put("text",q.text);put("correctAnswer",q.correctAnswer);put("answers",JSONArray(q.answers));put("category",q.category);put("difficulty",q.difficulty)})}
        val sa=JSONArray(); ss.forEach{s_->sa.put(JSONObject().apply{put("selectedAnswer",s_.selectedAnswer?:JSONObject.NULL);put("isAnswered",s_.isAnswered);put("isCorrect",s_.isCorrect);put("isWrong",s_.isWrong);put("isSkipped",s_.isSkipped);put("isFlagged",s_.isFlagged);put("isBookmarked",s_.isBookmarked);put("eliminatedAnswers",JSONArray(s_.eliminatedAnswers));put("isFiftyFiftyUsed",s_.isFiftyFiftyUsed)})}
        prefs.saveQuizSession(qa.toString(),sa.toString(),idx,strk,bst,!f50,!skp,!addT,b.categorySpinner.selectedItem?.toString()?:"Any Category",b.difficultySpinner.selectedItem?.toString()?:"Any Difficulty",qs.size)
    } catch(_:Exception){} }
    private fun lvd() { DialogHelper.showLeaveQuiz(this,{},{kt();sv();qOn=false;show("home")}) }
    override fun onBackPressed() { if(qOn)lvd() else super.onBackPressed() }

    private fun dq() { if(qs.isEmpty()||idx!in qs.indices)return; val q=qs[idx]; val s=ss[idx]
        b.questionProgress.text="QUESTION ${idx+1} / ${qs.size}"; b.quizProgressBar.max=qs.size; b.quizProgressBar.progress=idx+1
        rcs(); b.scoreText.text="Score: ${cs()}"; b.streakText.text="🔥 $strk"; b.flaggedCountText.text="🚩 ${ss.count{it.isFlagged}}"
        if(s.isFlagged){b.flagButton.setBackgroundColor(getColor(R.color.flag_orange));b.flagButton.setTextColor(Color.WHITE)}else{b.flagButton.setBackgroundColor(Color.TRANSPARENT);b.flagButton.setTextColor(getColor(R.color.chocolate_brown))}
        if(bm.isBookmarked(q.text)){b.bookmarkButton.setBackgroundColor(getColor(R.color.primary_pink));b.bookmarkButton.setTextColor(Color.WHITE)}else{b.bookmarkButton.setBackgroundColor(Color.TRANSPARENT);b.bookmarkButton.setTextColor(getColor(R.color.chocolate_brown))}
        b.questionText.text=q.text; val btns=listOf(b.answerButton1,b.answerButton2,b.answerButton3,b.answerButton4)
        btns.forEachIndexed{i,bt->val a=q.answers[i];bt.text=a;if(s.eliminatedAnswers.contains(a)){bt.visibility=View.INVISIBLE;bt.isEnabled=false}else{bt.visibility=View.VISIBLE;bt.isEnabled=!s.isAnswered}
            when{s.isAnswered&&a==s.selectedAnswer->{if(s.isCorrect){bt.setBackgroundColor(getColor(R.color.correct_green));bt.setTextColor(Color.WHITE)}else{bt.setBackgroundColor(getColor(R.color.wrong_red));bt.setTextColor(Color.WHITE)}}
            s.isAnswered&&a==q.correctAnswer->{bt.setBackgroundColor(getColor(R.color.correct_green));bt.setTextColor(Color.WHITE)} else->{bt.setBackgroundColor(Color.TRANSPARENT);bt.setTextColor(getColor(R.color.chocolate_brown))}} }
        if(s.isAnswered){b.feedbackText.visibility=View.VISIBLE;when{s.isCorrect->{b.feedbackText.text="Correct! ✓";b.feedbackText.setTextColor(getColor(R.color.correct_green))}
        s.isSkipped->{b.feedbackText.text="Skipped";b.feedbackText.setTextColor(getColor(R.color.skipped_gray))} else->{b.feedbackText.text="Wrong! ✗\nAnswer: ${q.correctAnswer}";b.feedbackText.setTextColor(getColor(R.color.wrong_red))}};btns.forEach{it.isEnabled=false}}
        else{b.feedbackText.visibility=View.GONE;btns.forEach{it.isEnabled=true}}
        b.previousButton.isEnabled=idx>0;b.previousButton.alpha=if(idx>0)1f else 0.5f;b.nextButton.text=if(idx==qs.size-1)"Finish"else"Next";uLL()
        if(!s.isAnswered)stT() else{kt();b.timerText.text="⏱ --"}}

    private fun ans(i: Int) { val q=qs[idx]; val s=ss[idx]; if(s.isAnswered)return; val a=q.answers[i]; val c=a==q.correctAnswer; s.selectedAnswer=a;s.isAnswered=true;s.isCorrect=c;s.isWrong=!c;kt()
        if(fa==0L)fa=System.currentTimeMillis(); if(c){strk++;if(strk>bst)bst=strk;snd.playCorrectSound();snd.vibrate(true)}else{strk=0;snd.playWrongSound();snd.vibrate(false)};dq() }
    private fun usk() { if(!skp)return; val s=ss[idx];if(s.isAnswered)return;skp=false;s.isAnswered=true;s.isSkipped=true;s.selectedAnswer=null;strk=0;kt();dq() }
    private fun tu() { val s=ss[idx];if(s.isAnswered)return;s.isAnswered=true;s.isWrong=true;s.selectedAnswer=null;strk=0;dq() }
    private fun pv() { if(idx>0){kt();idx--;dq();b.navigatorGrid.visibility=View.GONE} }
    private fun nx() { if(idx<qs.size-1){kt();idx++;dq();b.navigatorGrid.visibility=View.GONE}else{kt();finQ()} }

    private fun tnav() { if(b.navigatorGrid.visibility==View.VISIBLE){b.navigatorGrid.visibility=View.GONE;return}
        b.navigatorGrid.removeAllViews();b.navigatorGrid.visibility=View.VISIBLE;val cols=if(qs.size<=10)5 else 4;var row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        qs.forEachIndexed{i,_->val s=ss[i];val t=TextView(this).apply{text="${i+1}";setPadding(dp(6),dp(6),dp(6),dp(6));textSize=13f;setTypeface(null,Typeface.BOLD);gravity=Gravity.CENTER;layoutParams=LinearLayout.LayoutParams(dp(36),dp(36)).apply{marginEnd=dp(3);bottomMargin=dp(3)}
            when{i==idx->{setBackgroundColor(getColor(R.color.primary_pink));setTextColor(Color.WHITE)} s.isCorrect->{setBackgroundColor(getColor(R.color.correct_green));setTextColor(Color.WHITE)} s.isWrong->{setBackgroundColor(getColor(R.color.wrong_red));setTextColor(Color.WHITE)}
            s.isFlagged->{setBackgroundColor(getColor(R.color.flag_orange));setTextColor(Color.WHITE)} s.isSkipped->{setBackgroundColor(getColor(R.color.skipped_gray));setTextColor(Color.WHITE)} else->{setBackgroundColor(getColor(R.color.card_white));setTextColor(getColor(R.color.chocolate_brown))}}
            setOnClickListener{idx=i;dq();b.navigatorGrid.visibility=View.GONE}};row.addView(t);if((i+1)%cols==0||i==qs.size-1){b.navigatorGrid.addView(row);row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}}} }

    private fun uLL() { b.lifelineFiftyFifty.isEnabled=f50;b.lifelineFiftyFifty.alpha=if(f50)1f else 0.4f;b.lifelineSkip.isEnabled=skp;b.lifelineSkip.alpha=if(skp)1f else 0.4f;b.lifelineAddTime.isEnabled=addT;b.lifelineAddTime.alpha=if(addT)1f else 0.4f }
    private fun use50() { if(!f50)return;val s=ss[idx];if(s.isAnswered||s.isFiftyFiftyUsed)return;f50=false;s.isFiftyFiftyUsed=true;s.eliminatedAnswers=qs[idx].answers.filter{it!=qs[idx].correctAnswer}.shuffled().take(2);dq() }
    private fun uat() { if(!addT||!prefs.isTimerEnabled())return;addT=false;tl+=10000L;kt();stTW();dq() }
    private fun stT() { kt();if(!prefs.isTimerEnabled()){b.timerText.text="⏱ --";return};tl=prefs.getTimerDuration()*1000L;stTW() }
    private fun stTW() { b.timerText.text="⏱ ${tl/1000}s";tmr=object:CountDownTimer(tl,1000){override fun onTick(m:Long){tl=m;b.timerText.text="⏱ ${m/1000}s"}override fun onFinish(){tl=0;tmrOn=false;b.timerText.text="⏱ 0s";tu()}}.start();tmrOn=true }
    private fun kt(){tmr?.cancel();tmr=null;tmrOn=false}
    private fun cs()=ss.count{it.isCorrect}
    private fun rcs(){strk=0;for(i in 0..idx){if(ss[i].isCorrect)strk++else strk=0}}

    private fun finQ(){val c=ss.count{it.isCorrect};val w=ss.count{it.isWrong};val sk=ss.count{it.isSkipped};val fl=ss.count{it.isFlagged}
        prefs.saveQuizResult(b.categorySpinner.selectedItem?.toString()?:"Any Category",b.difficultySpinner.selectedItem?.toString()?:"Any Difficulty",qs.size,c,w,sk,bst,fl);if(daily) prefs.setDailyChallengeComplete(c,qs.size)
        ua(c,qs.size,bst);show("result")}
    private fun dR(){val c=ss.count{it.isCorrect};val w=ss.count{it.isWrong};val sk=ss.count{it.isSkipped};val fl=ss.count{it.isFlagged};val t=qs.size;val p=if(t>0)(c*100)/t else 0
        b.resultScoreLarge.text="$c / $t";b.resultPercentage.text="$p%";b.resultCorrectCount.text="$c";b.resultWrongCount.text="$w";b.resultSkippedCount.text="$sk";b.resultAccuracy.text="$p%";b.resultBestStreak.text="$bst";b.resultFlaggedCount.text="$fl"
        if(w>0&&!daily){b.practiceWrongButton.visibility=View.VISIBLE;b.practiceWrongButton.text="Practice $w Wrong Answers"}else b.practiceWrongButton.visibility=View.GONE}
    private fun ua(c:Int,t:Int,s:Int){prefs.unlockAchievement("first_quiz");if(s>=5)prefs.unlockAchievement("hot_streak");if(c==t&&t>0)prefs.unlockAchievement("perfect_score");if(prefs.getQuizzesPlayed()>=10)prefs.unlockAchievement("quiz_master");val e=System.currentTimeMillis()-fa;if(e in 1..5000)prefs.unlockAchievement("fast_thinker")}

    private fun fR(){b.reviewListContainer.removeAllViews();qs.forEachIndexed{i,q->val s=ss[i];val card=CardView(this).apply{layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply{bottomMargin=dp(8)};radius=dp(10).toFloat();cardElevation=dp(2).toFloat();setCardBackgroundColor(Color.WHITE)}
        val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(12),dp(14),dp(12))};c.addView(TextView(this).apply{text=when{s.isCorrect->"✓ CORRECT";s.isSkipped->"— SKIPPED";else->"✕ WRONG"};setTextColor(when{s.isCorrect->getColor(R.color.correct_green);s.isSkipped->getColor(R.color.skipped_gray);else->getColor(R.color.wrong_red)});textSize=13f;setTypeface(null,Typeface.BOLD)})
        c.addView(TextView(this).apply{text=q.text;setTextColor(getColor(R.color.chocolate_brown));textSize=15f;setTypeface(null,Typeface.BOLD);setPadding(0,dp(6),0,dp(4))});c.addView(TextView(this).apply{text="Your: ${s.selectedAnswer?:"Not answered"}";setTextColor(getColor(R.color.warm_brown));textSize=13f})
        if(!s.isCorrect)c.addView(TextView(this).apply{text="Correct: ${q.correctAnswer}";setTextColor(getColor(R.color.correct_green));textSize=13f;setTypeface(null,Typeface.BOLD)});card.addView(c);b.reviewListContainer.addView(card)}}
    private fun fF(){b.flaggedListContainer.removeAllViews();val fs=ss.filter{it.isFlagged};if(fs.isEmpty()){b.flaggedEmptyText.visibility=View.VISIBLE;return};b.flaggedEmptyText.visibility=View.GONE
        fs.forEach{s->val q=qs[s.questionIndex];val card=CardView(this).apply{layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply{bottomMargin=dp(8)};radius=dp(10).toFloat();cardElevation=dp(2).toFloat();setCardBackgroundColor(Color.WHITE);isClickable=true;isFocusable=true}
            val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(10),dp(14),dp(10))};c.addView(TextView(this).apply{text="🚩 Q${s.questionIndex+1}";setTextColor(getColor(R.color.flag_orange));textSize=13f;setTypeface(null,Typeface.BOLD)})
            c.addView(TextView(this).apply{text=q.text;setTextColor(getColor(R.color.chocolate_brown));textSize=14f;maxLines=2;setPadding(0,dp(2),0,0)});card.addView(c);card.setOnClickListener{idx=s.questionIndex;show("quiz")};b.flaggedListContainer.addView(card)}}
    private fun fH(){b.historyListContainer.removeAllViews();val h=prefs.getQuizHistory();if(h.isEmpty()){b.historyEmptyText.visibility=View.VISIBLE;return};b.historyEmptyText.visibility=View.GONE
        h.forEach{e->try{o@JSONObject(e);val card=CardView(this).apply{layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply{bottomMargin=dp(8)};radius=dp(10).toFloat();cardElevation=dp(2).toFloat();setCardBackgroundColor(Color.WHITE)}
            val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(10),dp(14),dp(10))};c.addView(TextView(this).apply{text=JSONObject(e).optString("category","?");setTextColor(getColor(R.color.chocolate_brown));textSize=15f;setTypeface(null,Typeface.BOLD)})
            val co=JSONObject(e).optInt("correct",0);val tot=JSONObject(e).optInt("totalQuestions",0);val p=if(tot>0)(co*100)/tot else 0
            c.addView(TextView(this).apply{text="$co / $tot • $p%";setTextColor(getColor(R.color.primary_pink));textSize=15f;setTypeface(null,Typeface.BOLD);setPadding(0,dp(2),0,0)});card.addView(c);b.historyListContainer.addView(card)}catch(_:Exception){}}}
    private fun fS(){b.statsContainer.removeAllViews();b.categoryPerfContainer.removeAllViews();b.recentPerfContainer.removeAllViews()
        listOf("Quizzes Played" to "${prefs.getQuizzesPlayed()}","Questions Answered" to "${prefs.getTotalQuestionsAnswered()}","Correct" to "${prefs.getTotalCorrect()}","Wrong" to "${prefs.getTotalWrong()}","Skipped" to "${prefs.getTotalSkipped()}","Accuracy" to "${prefs.getAccuracy()}%","Best Score" to "${prefs.getBestScore()} / ${prefs.getBestTotal()}","Best Streak" to "${prefs.getBestStreak()}","Average" to String.format("%.1f/10",prefs.getAverageScore())).forEach{(l,v)->
            val card=CardView(this).apply{layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply{bottomMargin=dp(8)};radius=dp(10).toFloat();cardElevation=dp(2).toFloat();setCardBackgroundColor(Color.WHITE)}
            val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(10),dp(14),dp(10))};c.addView(TextView(this).apply{text=l;setTextColor(getColor(R.color.warm_brown));textSize=12f;setTypeface(null,Typeface.BOLD)})
            c.addView(TextView(this).apply{text=v;setTextColor(getColor(R.color.chocolate_brown));textSize=20f;setTypeface(null,Typeface.BOLD)});card.addView(c);b.statsContainer.addView(card)}
        val cp=prefs.getCategoryPerformance();if(cp.isNotEmpty()){b.categoryPerfContainer.addView(TextView(this).apply{text="CATEGORY PERFORMANCE";setTextColor(getColor(R.color.chocolate_brown));textSize=14f;setTypeface(null,Typeface.BOLD);setPadding(0,0,0,dp(8))})
            cp.entries.sortedByDescending{if(it.value.second>0)(it.value.first*100)/it.value.second else 0}.forEach{(cat,p)->val pct=if(p.second>0)(p.first*100)/p.second else 0;val card=CardView(this).apply{layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply{bottomMargin=dp(6)};radius=dp(8).toFloat();cardElevation=dp(1).toFloat();setCardBackgroundColor(Color.WHITE)}
                val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(8),dp(12),dp(8))};c.addView(TextView(this).apply{text="$cat ${p.first}/${p.second}";setTextColor(getColor(R.color.chocolate_brown));textSize=13f;setTypeface(null,Typeface.BOLD)})
                c.addView(android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=100;progress=pct;layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(8)).apply{topMargin=dp(4)};progressTintList=android.content.res.ColorStateList.valueOf(getColor(R.color.primary_pink));progressBackgroundTintList=android.content.res.ColorStateList.valueOf(getColor(R.color.card_border))})
                card.addView(c);b.categoryPerfContainer.addView(card)}}}
    private fun fB(){b.bookmarksListContainer.removeAllViews();val items=bm.getBookmarkedQuestions();if(items.isEmpty()){b.bookmarksEmptyText.visibility=View.VISIBLE;return};b.bookmarksEmptyText.visibility=View.GONE
        items.forEach{item->val card=CardView(this).apply{layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply{bottomMargin=dp(8)};radius=dp(10).toFloat();cardElevation=dp(2).toFloat();setCardBackgroundColor(Color.WHITE);isClickable=true;isFocusable=true}
            val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(10),dp(14),dp(10))};c.addView(TextView(this).apply{text="🔖 ${item.category} • ${item.difficulty}";setTextColor(getColor(R.color.warm_brown));textSize=12f})
            c.addView(TextView(this).apply{text=item.questionText;setTextColor(getColor(R.color.chocolate_brown));textSize=14f;setTypeface(null,Typeface.BOLD);maxLines=3;setPadding(0,dp(4),0,0)});c.addView(TextView(this).apply{text="✓ ${item.correctAnswer}";setTextColor(getColor(R.color.correct_green));textSize=13f;setPadding(0,dp(2),0,0)})
            card.addView(c);card.setOnClickListener{val q=Question(item.questionText,item.correctAnswer,item.answers,item.category,item.difficulty);qs=listOf(q);ss=mutableListOf(QuizState(0));idx=0;f50=false;skp=false;addT=false;show("quiz")};b.bookmarksListContainer.addView(card)}}
    private fun fA(){b.achievementsListContainer.removeAllViews()
        listOf("first_quiz" to ("First Quiz" to "Complete your first quiz"),"hot_streak" to ("Hot Streak" to "Get 5 correct in a row"),"perfect_score" to ("Perfect Score" to "Answer every question correctly"),"quiz_master" to ("Quiz Master" to "Complete 10 quizzes"),"fast_thinker" to ("Fast Thinker" to "Answer questions quickly")).forEach{(k,t)->val u=prefs.isAchievementUnlocked(k)
            val card=CardView(this).apply{layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply{bottomMargin=dp(8)};radius=dp(10).toFloat();cardElevation=dp(2).toFloat();setCardBackgroundColor(Color.WHITE)}
            val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(10),dp(14),dp(10))};c.addView(TextView(this).apply{text="${if(u)"✓"else"🔒"} ${t.first}";setTextColor(if(u)getColor(R.color.correct_green)else getColor(R.color.text_secondary));textSize=15f;setTypeface(null,Typeface.BOLD)})
            c.addView(TextView(this).apply{text=t.second;setTextColor(getColor(R.color.warm_brown));textSize=12f;setPadding(0,dp(2),0,0)});c.addView(TextView(this).apply{text=if(u)"Unlocked"else"Locked";setTextColor(if(u)getColor(R.color.correct_green)else getColor(R.color.text_secondary));textSize=11f;setTypeface(null,Typeface.BOLD);setPadding(0,dp(2),0,0)})
            card.addView(c);b.achievementsListContainer.addView(card)}}

    private fun startP(){pqs=qs.filterIndexed{i,_->ss[i].isWrong};if(pqs.isEmpty())return;pi=0;pc=0;pa=false;listOf(b.practiceAnswer1,b.practiceAnswer2,b.practiceAnswer3,b.practiceAnswer4).forEachIndexed{i,bt->bt.setOnClickListener{hpa(i)}};show("practice")}
    private fun dP(){if(pi>=pqs.size)return;val q=pqs[pi];pa=false;b.practiceProgress.text="PRACTICE ${pi+1} / ${pqs.size}";b.practiceProgressBar.max=pqs.size;b.practiceProgressBar.progress=pi+1;b.practiceQuestionText.text=q.text
        val btns=listOf(b.practiceAnswer1,b.practiceAnswer2,b.practiceAnswer3,b.practiceAnswer4);btns.forEachIndexed{i,bt->bt.text=q.answers[i];bt.setBackgroundColor(Color.TRANSPARENT);bt.setTextColor(getColor(R.color.chocolate_brown));bt.isEnabled=true;bt.visibility=View.VISIBLE}
        b.practiceFeedback.visibility=View.GONE;b.practiceNextButton.text="Next";b.practiceNextButton.setOnClickListener{if(!pa)return@setOnClickListener;pi++;if(pi<pqs.size)dP() else{b.practiceFeedback.text="PRACTICE COMPLETE\n$pc / ${pqs.size}";b.practiceFeedback.setTextColor(getColor(R.color.correct_green));b.practiceFeedback.visibility=View.VISIBLE;b.practiceNextButton.text="Back";b.practiceNextButton.setOnClickListener{show("result")}}}}
    private fun hpa(i:Int){if(pa)return;pa=true;val q=pqs[pi];val btns=listOf(b.practiceAnswer1,b.practiceAnswer2,b.practiceAnswer3,b.practiceAnswer4)
        val c=q.answers[i]==q.correctAnswer;if(c)pc++;btns.forEach{bt->bt.isEnabled=false;if(bt.text.toString()==q.correctAnswer){bt.setBackgroundColor(getColor(R.color.correct_green));bt.setTextColor(Color.WHITE)}}
        if(!c){btns[i].setBackgroundColor(getColor(R.color.wrong_red));btns[i].setTextColor(Color.WHITE)};b.practiceFeedback.text=if(c)"Correct! ✓"else"Wrong! ✗";b.practiceFeedback.setTextColor(getColor(if(c)R.color.correct_green else R.color.wrong_red));b.practiceFeedback.visibility=View.VISIBLE}

    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onDestroy(){super.onDestroy();kt();snd.release()}
}
