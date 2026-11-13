cmake_minimum_required(VERSION 3.13)
project(kcanotify)

add_library( netguard
             SHARED
             src/main/jni/netguard/netguard.c
             src/main/jni/netguard/session.c
             src/main/jni/netguard/ip.c
             src/main/jni/netguard/tcp.c
             src/main/jni/netguard/udp.c
             src/main/jni/netguard/icmp.c
             src/main/jni/netguard/tls.c
             src/main/jni/netguard/dns.c
             src/main/jni/netguard/dhcp.c
             src/main/jni/netguard/util.c )

include_directories( src/main/jni/netguard/ )

find_library( log-lib
              log )

target_link_libraries( netguard
                       ${log-lib} )

# 16 KB page size devices need 16 KB aligned LOAD segments
target_link_options( netguard
                     PRIVATE
                     "-Wl,-z,max-page-size=16384"
                     "-Wl,-z,common-page-size=16384" )
