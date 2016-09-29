#!/bin/bash

source build/envsetup.sh
lunch aosp_x86-eng
make -j8 WITH_DEXPREOPT=false | tee build_generic_x86.log
