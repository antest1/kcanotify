<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@40,400,0,0&icon_names=vpn_key" />

## 안드로이드판 칸코레 연동
안드로이드판 칸코레와 함께 사용하기 위한 설정 방법입니다.

<div class="alert alert-warning" role="alert">
현재 DMM에서 해외 접속을 차단하고 있기 때문에, 로그인하기 위해서는 별도의 VPN 사용이 필요합니다.</br>
<span class="text-danger">깡들리티에서는 지역 차단 우회를 위한 VPN 기능을 제공하지 않습니다.</span>
</div>

##### 설정 방법
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ko/officialapp_integration.png" width="720"/>

설정 → 깡들리티 설정에서 다음과 같이 설정합니다.
  - 칸코레 어플리케이션 선택: <span class="text-danger">안드로이드판 칸코레 (공식)</span>
  - 해당 설정은 서비스가 꺼져 있는 상태에서만 변경이 가능합니다.

**깡들리티에서 게임 통신 내용을 읽을 수 있도록 게임 어플을 패치해야 합니다.**
- 자세한 내용은 **칸코레 어플 패치 방법**을 참고하세요.

스니퍼(ACTIVE) 설정에서 <span class="text-danger">HTTPS 트래픽 캡쳐</span>를 활성화합니다.
  - <b>Mitm 설치 도우미</b>를 통해 필요한 항목들을 먼저 설치해야 합니다. (하단 내용 참고)
  - 필요한 항목들이 설치되지 않은 경우 스니퍼 시작 시 도우미 화면이 나타납니다.

---

##### 플레이 방법
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ko/active_sniffer_buttons.png" width="480"/>

- 메인 상단의 2개 버튼(스니퍼/서비스)을 On 상태로 변경한 이후, 하단의 GAME START 버튼을 눌러 게임을 실행합니다.
  - 정상적으로 동작하는 상태인 경우 상태바에 열쇠 모양(<span class="material-symbols-outlined inline-icon">vpn_key</span>)이 나타납니다.
  - 게임 화면이 감지되면 화면 위에 요정 버튼이 나타납니다.
- 처음 실행하는 경우 다음 설정들이 나타날 수 있습니다. (하단 스크린샷 참고)
  - VpnService 관련 안내를 수락한 이후, 연결 요청을 확인
  - kcanotify mitm addon의 배터리 최적화 해제
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ko/sniffer_init.png" width="800"/>

---

##### 칸코레 어플 패치 방법

<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/common/revanced_icon.png" width="96"/>

이 가이드에서는 **ReVanced Manager**를 활용하여 원본 APK 파일을 패치하는 방법에 대해 설명합니다.  
공식 웹사이트에서 ReVanced Manager를 다운로드받을 수 있습니다.

🔗 [공식 웹사이트](https://revanced.app/)

###### 설치 및 설정

<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ko/revanced_settings.png" width="360"/>

ReVanced Manager를 실행한 이후,  
설정에서 <span class="text-danger">패치 선택 변경 허용</span> 및
<span class="text-danger">공용 패치 보기</span>를 활성화합니다.

###### 앱 및 패치 선택하기

<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ko/revanced_patcher.png" width="640"/>

화면 하단의 Patcher를 선택합니다.

우선, "앱 선택하기"에서 칸코레 어플을 찾은 후 선택합니다.  
🔍 "艦これ" 혹은 "kancolle" 키워드로 검색 가능

이후, "패치 선택하기"에서 <span class="text-danger">「Override certificate pinning」</span>을 선택합니다.

###### 패치 진행 및 설치

<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ko/revanced_progress.png" width="640"/>

선택 내용 확인 이후, 우하단 패치하기를 눌러 진행합니다.  
패치가 완료된 이후, 어플을 다시 설치하면 됩니다.

💡 기존에 설치된 칸코레는 삭제되어야 합니다.  
📢 DMM Store로부터 업데이트를 다운받고 설치할 때,  
기존의 어플을 삭제한 이후 다시 패치를 수행해야 합니다.

---

##### Mitm 설치 도우미

###### kcanotify mitm addon 어플 설치
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/common/mitm_icon.png" width="96"/>

- 깡들리티로부터 전달받은 칸코레 관련 암호화된 통신 데이터를 중간에서 복호화하는 역할을 수행합니다.
  - 해당 어플리케이션의 소스 코드는 [Github Repository](https://github.com/antest1/kcanotify-mitm)에 공개되어 있습니다.
- 하단의 <span class="text-danger">설치</span> 버튼을 클릭하면 APK 설치 파일을 다운받을 수 있습니다. (위의 아이콘 확인)
- 설치 후 깡들리티로 돌아왔을 때, 정상적으로 연동된 경우 아래 예시와 같이 화면에서 ✔️ 아이콘이 표시됩니다.
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ko/mitm_addon_install.png" width="640"/>

###### CA 인증서 설치
- 암호화된 통신 데이터를 복호화하기 위해 해당 인증서가 설치되어야 합니다.
  - 안드로이드 보안 정책으로 인해 수동 설치 필요
  - 시제 깡들리티에서는 칸코레 게임 플레이 관련 데이터 외의 개인정보에 접근하거나 무단 활용하지 않습니다.

<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ko/certificate_install.png" width="640"/>

- 하단의 <span class="text-danger">내보내기</span> 버튼을 클릭하여 <code>Kcanotify_CA.crt</code> 파일을 저장합니다.
- 기기의 설정에서 <span class="text-danger">"인증서"</span>를 검색해서 나오는 항목 중 <span class="text-danger">"CA 인증서"</span>를 선택한 이후, 저장된 인증서를 선택하여 설치합니다.
  - ※ 최신 갤럭시 기기의 경우 <span class="text-primary">보안 및 개인정보 보호 → 기타 보안 설정 → 기기에 저장된 인증서 설치 → CA 인증서</span>
  - 해당 인증서를 삭제하고자 하는 경우 "인증서 확인" 설정에서 "사용자" 탭을 클릭한 이후, 해당 인증서를 선택해서 삭제할 수 있습니다.
- 정상적으로 설정된 경우 아래 예시와 같이 화면에서 ✔️ 아이콘이 표시됩니다.
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ko/certificate_ok.png" width="320"/>