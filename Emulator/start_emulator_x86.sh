#!/bin/bash

SCRIPT_DIR=`dirname $0`
MAINHOME=`readlink -f $SCRIPT_DIR`
SDKHOME=/extra/android/adt-bundle-linux-x86_64-20140702/sdk
ANDROID_SRC_HOME=/extra/android/source/android-4.3_r1
EMULATOR_HOME=$MAINHOME/emulator_x86
ANDROID_HOST_OUT=$ANDROID_SRC_HOME/out/host/linux-x86
ANDROID_PRODUCT_OUT=$ANDROID_SRC_HOME/out/target/product/generic_x86
SDCARD=$ANDROID_SRC_HOME/yhu009/sdcard1.iso
AVD_NAME=valera_x86.avd

USE_KVM=true

#source build/envsetup.sh
#lunch aosp_x86-eng

if [ $USE_KVM == true ]; then
	echo "Starting emulator with KVM enabled"
else
	echo "Starting emulator with KVM disabled. Note that as a result emulator will likely be very slow."
fi	

#tell emulator where to find valera AVD
AVD_HOME=$EMULATOR_HOME/.android/avd
mkdir -p $AVD_HOME/$AVD_NAME
export ANDROID_SDK_HOME=$EMULATOR_HOME
echo "ANDROID_SDK_HOME=$ANDROID_SDK_HOME"

#for emulator to find openGL libaries
export LD_LIBRARY_PATH="$ANDROID_HOST_OUT/lib:$LD_LIBRARY_PATH"

echo "AVDHOME = $AVD_HOME"

echo "avd.ini.encoding=ISO-8859-1" > $AVD_HOME/valera_x86.ini
echo "path=$EMULATOR_HOME/.android/avd/$AVD_NAME" >> $AVD_HOME/valera_x86.ini
echo "path.rel=avd/$AVD_NAME" >> $EMULATOR_HOME/.android/avd/valera_x86.ini
echo "target=android-18" >> $EMULATOR_HOME/.android/avd/valera_x86.ini

cp $MAINHOME/config/config_base.ini $AVD_HOME/$AVD_NAME/config.ini
echo "image.sysdir.1=$ANDROID_PRODUCT_OUT" >> $AVD_HOME/$AVD_NAME/config.ini

if [ $USE_KVM == true ]; then
	$ANDROID_HOST_OUT/bin/emulator -avd valera_x86 -skindir $SDKHOME/platforms/android-18/skins -skin WVGA800 -sdcard $SDCARD -kernel $ANDROID_SRC_HOME/prebuilts/qemu-kernel/x86/kernel-qemu -qemu -m 1024 -enable-kvm
else
	$ANDROID_HOST_OUT/bin/emulator -avd valera_x86 -skindir $SDKHOME/platforms/android-18/skins -skin WVGA800 -sdcard $SDCARD -kernel $ANDROID_SRC_HOME/prebuilts/qemu-kernel/x86/kernel-qemu
fi
