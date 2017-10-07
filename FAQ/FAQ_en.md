## FAQ (English)

If you have any question, please mail to developer. (kcanotify@gmail.com)

Kcanotify is supposed to use with smartphone. It can also be used with android tablet, but not supported officially.  
(since I don't have tablet...)

---

#### 0. How to use it?
Storage Permission and Screen Overlay Permission must be granted before using kcanotify. You just activate VPN and SVC button and start kancolle by pressing START GAME button. (You don't need to start kancolle in Kcanotify) Fairy will be appeared when Kcanotify is on, and just touch the fairy to use Kcanotify's features.

#### 1. Do I need to pay for Kcanotify?
No, All features of Kcanotify is offered **Free**. It does not contains Ads nor in-app purchases.

#### 2. How it works?
Kcanotify retrieves data from kancolle with Local VPN, which is from the open-source no-root firewall **NetGuard** with some modification. 
The reason for using local VPN is that it is (almost) the only way to capture the packets without root permission. 
After obtains kacolle data, Kcanotify properly interpret and represent with screen overlay like other kancolle viewers for PC.
You can check about FAQ for NetGuard in [this document](https://github.com/M66B/NetGuard/blob/master/FAQ.md).

#### 3. Can I get banned by using this application?
This application does not modify traffics and send any generated packet to DMM/Kancolle server. (It means that this application does not has macro/cheet functionality) For transparency, whole source code of kcanotify is available in Github. Also, since this application runs local VPN in your device, IP address is not modified. Developer played this game for long time, and get ranking rewards multiple times without any neko-bomb(201 cat). While more than 10,000 people are using this application, there was not any reported that they got banned when using kcanotify. However, using kcanotify with other android-based macro/cheat tools (though I don't know even it exists) may lead to be banned, and I'll not take any responsibility for this case.

#### 4. Can I use this application with other 3rd-party VPN application?
No, you can't. Since kcanotify is using VpnService to sniff kancolle data, and Android system does not allow to use more than two VPN services. For IP bypass, Kcanotify supports [SOCKS5 proxy](https://en.wikipedia.org/wiki/SOCKS).

#### 5. Kcanotify consumes a lot of traffic, according to Android built-in statistics.
Since in the process of routing all the mobile traffic goes through Kcanotify VPN (and filter kancolle data), Android decides that it is Kcanotify that consumes it all. Of course, this is not true, since Kcanotify does not make any traffic except update check or sending statistic to OpenDB(off in default), which consumes very little traffic. It shows traffic that you used while Kcanotify is on. You may check [this document](https://kb.adguard.com/en/android/solving-problems/battery), which dealing with similar issue.

#### 6. Kcanotify consumes a lot of battery.
It is no secret that this is currently the main drawback of Kcanotify. Since this application usually being on for relately long time and running as forward service, this application use much battery. You can think as music app drains the battery when you use it long time. I'm trying to deal with this problem, but it seems to not be solve in short time. You may turn off the Sniffer and Service when you does not using Kcanotify. **(you can get notification even if you turn off service.)**

#### 7. The mail client (com.antest1.kcanotify Crash Report) or toast error message appears when using Kcanotify.
It means that there is a problem with Kcanotify. It would be help to send me the crash report to solve bugs in Kcanotify. 
When you report the error message, please send me the error log, which can be found in right-top menu in Kcanotify main screen.

#### 8. It shows the message that game data is not latest. (There is no data for current api version in kcanotify server)
Game data for kcanotify is updated manually by developer when there is an update for kancolle(e.g. maintenance), since automated request for game data(api_start2) can lead to be banned. It may not be uploaded immediately, since sometimes DNKS update api_start2 version without any tweets. In this case, please mail to me then I'll check it as fast as possible. You can check the new data update in the main screen of kcanotify(blue notification).

#### 9. Why I have to download the game data manually?
Unlike web port of kancolle, android port does not download game data (api_start2) everytime but save it in internal cache unless there is no new version, and external application cannot access to it. You may clear the cache whenever you play kancolle, but it make kancolle application to download large image/audio/flash files leading consume large traffics, which throws the benefits of the kancolle android port when using a 3G/4G network. 
Therefore, I upload the game data to data server(http://swaytwig.com/kcanotify/) when there is an update. you can check the game data at [Here](https://github.com/antest1/kcanotify/blob/master/app/src/main/assets/api_start2).

#### 10. (for Android 6.0+) There is a message in the red box (Screen Overlay Permission is required for... )
Go to **Settings - Permission/BattleView Settings - Screen Overlay Settings** and you can enter to the screen for the overlay permission setting.

#### 11. I want to hide the fairy outside of kancolle.
Go to **Settings - Permission/BattleView Settings - Accessibility Settings** and activate Kcanotify in service section. This enables Kcanotify to check whether kancolle is on the screen and automatically hides the fairy. You can also touch fairy long time to hide fairy (She will say bye-bye.) You can reload the fairy by click fairy button (next to start game button) in the main screen of kcanotify.

#### 13. Kcanotify does not work with my device! (Fairy does not appears, etc)
The reason why the application does not work vary from device to device, and developer can not always test it on various devices, so it is not always possible to provide correct solutions. 
Especially, in case of some devices which is not in Korea, there is no way to test it, so I have to guess the reason through some messages (like stack trace). Therefore, when an error message occurs (like in mail client), please actively send mail to the developer and I'll inform you of the appropriate action as soon as possible.

This is the common cases.
- Using Custom ROM: VPN feature is missing in some ROMs. Since Kcanotify works based on VpnService, you may use other ROMs.
- Error message occurs which containing OutOfMemory: it may appeared in some low-end devices. Report to me if this problem occures frequently.
- Suddenly, the mail client (containing some text like error message) appears: See 7.

#### 14. Why this permission is required? Isn't it leaking my personal information?
This is the list of permissions which Kcanotify requires.
- *INTERNET*: to forward traffic captured by Local VPN to the internet.
- *VIBRATE*: to provide vibration feedback
- *ACCESS_WIFI_STATE*: to detect Wi-Fi network changes
- *WAKE_LOCK*: to check connectivity changes and provide notification in the background
- *SYSTEM_ALERT_WINDOW*: to show the screen overlay
- *READ/WRITE_EXTERNAL_STORAGE*: to use custom ringtones and save error logs (It does not need to be set, but unexpected behaviors may occurs, please report to me in this case)

You can check the [Privacy Policy](https://github.com/antest1/kcanotify/blob/master/private_policy.md) here.

#### 15. VPN in kcanotify does not started after updates.
In some devices/android versions, there is a bug that does not request permission to start VPN. This is temporal, so you may restart the device or reinstall the application after turn off the Sniffer(VPN) in Kcanotify.
Also, you may goto **System Settings - More Network - VPN** (The methods can vary: this is the case of Samsung devices) and remove Kcanotify, and re-gain the permission when start Kcanotify.

#### 16. Please make ~~~ features!
If you want to suggest some features, you can send the mail to developer or make [Issue](https://github.com/antest1/kcanotify/issues) in Github. I'll try to add it as 
I'll try to reflect as much as possible in the case of good ideas within the scope of the competence and time of the developer. 
However, please be aware that developer is graduate student and can not always update quickly unless serious bug fixes needed.

#### 17. I want to contribute to this project.
Pull requests for feature additions and bug fixes are always welcome. To prevent duplicated work, please contact to the developer first (by E-mail or Issue) so that I can aware about. For PR, I'll reflect good updates after review.

#### 18. I want to translate the text in this application. / I found mistranslation.
I can speak Korean and English, and can read Japanese. For other languages like Chinese, I'm being helped by some contributers and Google Translations.
Translations for in-game texts like ship names and quests is from [kc3-translations](https://github.com/KC3Kai/kc3-translations), [Kancolle Wikia](kancolle.wikia.com), and [舰娘百科](zh.kcwiki.moe).
For these reasons, the translations for English/Chinese can be delayed then Korean. 
You can send translations by e-mail or Pull Request if you want fast update or to fix in-app text translation.
