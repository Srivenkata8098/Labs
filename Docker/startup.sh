#!/bin/sh

echo "================= Container Health Report ================="
echo "Hostname      : $(hostname)"
echo "Container IP  : $(hostname -i)"
echo "Uptime        : $(uptime -p)"
echo "Current Time  : $(date)"
echo "------------------------------------------------------------"
echo "CPU Load      : $(cat /proc/loadavg | awk '{print $1, $2, $3}')"
echo "Memory Usage  :"
free -h | awk 'NR==1 || NR==2 {print $1, $2, $3, $4}'
echo "------------------------------------------------------------"
echo "Disk Usage    :"
df -h | grep -E '^/dev'
echo "============================================================"

