#!/bin/bash
#
# Copyright (C) 2008 SIPfoundry Inc.
# Licensed by SIPfoundry under the LGPL license.
#
# Copyright (C) 2008 Pingtel Corp.
# Licensed to SIPfoundry under a Contributor Agreement.

. /opt/sipx-0.0.4.5.2/libexec/sipXecs/sipx-utils.sh

# Ensure that important FreeSWITCH files are writeable by dhubler
FS_DBDIR=/opt/sipx-0.0.4.5.2/var/sipxdata/tmp/freeswitch

if test ! -e $FS_DBDIR
then
    mkdir $FS_DBDIR
    chown -R dhubler $FS_DBDIR
    chgrp -R dhubler $FS_DBDIR
    chmod -R u+rwX,ga+rX $FS_DBDIR
fi

# Ensure that the audio devices are owned by dhubler
# This is required for proper FreeSWITCH operation.
for dev in /dev/adsp /dev/audio /dev/dsp /dev/mixer /dev/sequencer /dev/sequencer2 ; do
    if test -e $dev
    then
        chown dhubler $dev
    fi
done
if test -d /dev/snd
then
    chown dhubler /dev/snd/*
fi

# If alsa present configure the sound input source used for MOH to be 'Line'
if test -d /dev/snd
then
    amixer cset iface=MIXER,name="Input Source",index=0 "Line" >& /dev/null
    amixer cset iface=MIXER,name="Capture Switch",index=0 on >& /dev/null
    amixer cset iface=MIXER,name="Capture Volume",index=0 60% >& /dev/null
fi


