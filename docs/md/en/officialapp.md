<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@40,400,0,0&icon_names=vpn_key" />

## Official App Integration
Here's how to set up Kcanotify to use with Kancolle Android Version.

<div class="alert alert-warning" role="alert">
Since DMM currently blocks access from countries other than Japan, you may need a different VPN to log in.<br/>
<span class="text-danger">Kcanotify does not provide a VPN feature for bypassing regional blocks.</span>
</div>

##### Prerequisite
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/en/officialapp_integration.png" width="720"/>

From Settings → Kcanotify Settings:
  - Select KanColle Application: <span class="text-danger">KanColle Android (Official)</span>
  - These settings can only be changed when the service is turned off.


**You must patch the official app to enable kcanotify to read game traffic.**  
  - check **Instruction for Patching Official App** section for detail

From Sniffer(ACTIVE) Settings, enable <span class="text-danger">Capture HTTPS traffic</span>.
  - You must first install the required components via the <b>Mitm Setup Wizard</b>. (check below instructions)
  - If the required components are not installed, a wizard screen will appear when you start Sniffer.


---

##### How to Play
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/en/active_sniffer_buttons.png" width="480"/>

- After turning on the two buttons (Sniffer/Service) at the top of the main, press the GAME START button at the bottom.
  - If the app is working properly, a key icon (<span class="material-symbols-outlined inline-icon">vpn_key</span>) appears in the status bar.
  - When the game is detected, a fairy button will appear on the screen.
- When you run it for the first time, the following settings may appear. (check the screenshot below)
  - Allow the connection request after accepting the VpnService related dialog
  - Turn off battery optimization for the kcanotify mitm addon
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/en/sniffer_init.png" width="800"/>

---

##### Instruction for Patching Official App

<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/common/revanced_icon.png" width="96"/>

This guide explains how to apply a patch to
the original APK file using **ReVanced Manager**.  
You can download ReVanced Manager from the official website.

🔗 [Official Website](https://revanced.app/)

###### Installation and Settings

<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/en/revanced_settings.png" width="360"/>

Install and launch ReVanced Manager.  
In the settings, enable
<span class="text-danger">Allow changing patch selection</span> and
<span class="text-danger">Show universal patches</span>.

###### Selecting target app and patches

<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/en/revanced_patcher.png" width="640"/>

Open **Patcher** at the bottom of the screen.

First, find and select the KanColle app from "Select an app" section.  
🔍 You can also search for "艦これ" or "kancolle".

Next, select <span class="text-danger">「Override certificate pinning」</span> from "Select patches" section.

###### Run Patch and Install the Patched App

<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/en/revanced_progress.png" width="640"/>

Review the details and click "Patch".
After the apk file is patched, install the patched apk.

💡 Uninstall existing app before reinstalling.  
📢 If there is an update via the DMM Store,  
delete the old patched version and apply the patch again.

---

##### Mitm Setup Wizard
###### Install kcanotify mitm addon
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/common/mitm_icon.png" width="96"/>

- This addon performs the role of decrypting HTTPS traffic related to KanColle received from Kcanotify.
  - The source code of the application is available in the [Github Repository](https://github.com/antest1/kcanotify-mitm).
- You can download the APK file by clicking the <span class="text-danger">Install</span> button below. (Check the icon above)
- If the addon is successfully integrated, you will see a ✔️ icon on the screen as below when you return to Kcanotify.
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/en/mitm_addon_install.png" width="640"/>

###### Install CA Certificate
- The certificate must be installed to decrypt encrypted KanColle-related traffic.
  - Due to Android security policy, users must manually install the certificate on their devices.
  - Kcanotify does not access or use any personal information other KanColle game play information.

<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/en/certificate_install.png" width="640"/>

- Click the <span class="text-danger">Export</span> button below and save the <code>Kcanotify_CA.crt</code> file.
- Search for <span class="text-danger">"certificate"</span> in the device settings, then select <span class="text-danger">"CA certificate"</span> from the items that appear, and then select the saved certificate to install it.
  - ※ Example: <span class="text-primary">Security & privacy → More security & privacy → Encryption & credentials → Install a certificate</span>
  - If you want to delete the certificate, you can click and remove it from the "User" tab in the "Trusted credentials" settings.
- If the certificate is successfully installed, you will see a ✔️ icon on the screen as below when you return to Kcanotify.
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/en/certificate_ok.png" width="320"/>