# NOTE: Prefer Python script when target MySQL rejects GENERATED column inserts:
#   python scripts/migrate_db_preserve_ids.py --yes
# This mysqldump path may fail on STORED GENERATED columns (e.g. pending_slot).
#Requires -Version 5.1
<#
.SYNOPSIS
  Migrate full basketball DB from local to public test server. IDs / AUTO_INCREMENT preserved.

.DESCRIPTION
  Source: 192.168.31.48 / basketball
  Target: 111.229.241.29 / basketball
  Uses mysqldump --databases (tables + data + triggers + routines + events).
  Target DB will be overwritten.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts/migrate_db_local_to_remote.ps1 -Yes
#>

[CmdletBinding()]
param(
    [switch]$Yes,
    [string]$MysqlBin = "C:\Program Files\MariaDB 12.3\bin",
    [string]$DumpDir = ""
)

$ErrorActionPreference = "Stop"

$SrcHost = "192.168.31.48"
$SrcPort = 3306
$SrcUser = "root"
$SrcPass = "123456"
$SrcDb   = "basketball"

$DstHost = "111.229.241.29"
$DstPort = 3306
$DstUser = "root"
$DstPass = "zlzcnpa@@qaq"
$DstDb   = "basketball"

$mysqldump = Join-Path $MysqlBin "mysqldump.exe"
$mysql     = Join-Path $MysqlBin "mysql.exe"

if (-not (Test-Path $mysqldump)) { throw "mysqldump not found: $mysqldump" }
if (-not (Test-Path $mysql))     { throw "mysql not found: $mysql" }

if (-not $DumpDir) {
    $DumpDir = Join-Path $PSScriptRoot ("_db_migrate_" + (Get-Date -Format "yyyyMMdd_HHmmss"))
}
New-Item -ItemType Directory -Force -Path $DumpDir | Out-Null
$dumpFile = Join-Path $DumpDir ("{0}_full.sql" -f $SrcDb)

Write-Host "=== CNPA DB migrate (preserve IDs) ===" -ForegroundColor Cyan
Write-Host "Source : ${SrcUser}@${SrcHost}:${SrcPort}/${SrcDb}"
Write-Host "Target : ${DstUser}@${DstHost}:${DstPort}/${DstDb}"
Write-Host "Dump   : $dumpFile"
Write-Host ""
Write-Host "WARNING: target database will be overwritten." -ForegroundColor Yellow

if (-not $Yes) {
    $ans = Read-Host "Type YES to continue"
    if ($ans -ne "YES") {
        Write-Host "Aborted."
        exit 1
    }
}

function Invoke-MysqlExe {
    param(
        [string]$HostName,
        [int]$Port,
        [string]$User,
        [string]$Password,
        [string[]]$ExtraArgs = @(),
        [string]$Sql = "",
        [string]$InputFile = ""
    )
    $allArgs = @(
        "-h", $HostName,
        "-P", "$Port",
        "-u", $User,
        "--password=$Password",
        "--default-character-set=utf8mb4 --skip-ssl-verify-server-cert"
    ) + $ExtraArgs

    if ($InputFile) {
        $proc = Start-Process -FilePath $mysql -ArgumentList ($allArgs + @($DstDb)) -RedirectStandardInput $InputFile -NoNewWindow -Wait -PassThru
        # Prefer: mysql < file via cmd for large dumps
        return
    }
    if ($Sql) {
        & $mysql @allArgs -e $Sql
        if ($LASTEXITCODE -ne 0) { throw "mysql -e failed, exit=$LASTEXITCODE" }
        return
    }
    throw "need -Sql or use restore path"
}

Write-Host "`n[1/4] Ping source / target..."
& $mysql -h $SrcHost -P "$SrcPort" -u $SrcUser "--password=$SrcPass" --default-character-set=utf8mb4 --skip-ssl-verify-server-cert -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$SrcDb';"
if ($LASTEXITCODE -ne 0) { throw "cannot connect source" }
& $mysql -h $DstHost -P "$DstPort" -u $DstUser "--password=$DstPass" --default-character-set=utf8mb4 --skip-ssl-verify-server-cert -N -e "SELECT 1;"
if ($LASTEXITCODE -ne 0) { throw "cannot connect target" }

Write-Host "`n[2/4] Dump source database..."
& $mysqldump `
  -h $SrcHost -P "$SrcPort" -u $SrcUser "--password=$SrcPass" `
  --default-character-set=utf8mb4 --skip-ssl-verify-server-cert `
  --single-transaction --quick `
  --routines --triggers --events `
  --add-drop-database `
  --databases $SrcDb `
  "--result-file=$dumpFile"
if ($LASTEXITCODE -ne 0) { throw "mysqldump failed, exit=$LASTEXITCODE" }

$dumpSize = (Get-Item -LiteralPath $dumpFile).Length
if ($dumpSize -lt 100) { throw "dump file too small: $dumpFile ($dumpSize bytes)" }
Write-Host ("Dump OK: {0} MB" -f [math]::Round($dumpSize/1MB, 2))

Write-Host "`n[3/4] Restore into target (overwrite)..."
# Use cmd redirection for large SQL reliably on Windows
$dstPassEsc = $DstPass.Replace('"', '""')
$cmd = "`"$mysql`" -h $DstHost -P $DstPort -u $DstUser --password=`"$dstPassEsc`" --default-character-set=utf8mb4 --skip-ssl-verify-server-cert < `"$dumpFile`""
cmd.exe /c $cmd
if ($LASTEXITCODE -ne 0) { throw "mysql restore failed, exit=$LASTEXITCODE" }
Write-Host "Restore OK"

Write-Host "`n[4/4] Verify row counts / max ids..."
function Get-Scalar([string]$HostName, [int]$Port, [string]$User, [string]$Password, [string]$Sql) {
    $out = & $mysql -h $HostName -P "$Port" -u $User "--password=$Password" --default-character-set=utf8mb4 --skip-ssl-verify-server-cert -N -e $Sql 2>$null
    if ($LASTEXITCODE -ne 0) { return $null }
    return ("$out").Trim()
}

$srcTables = Get-Scalar $SrcHost $SrcPort $SrcUser $SrcPass "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$SrcDb' AND table_type='BASE TABLE'"
$dstTables = Get-Scalar $DstHost $DstPort $DstUser $DstPass "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DstDb' AND table_type='BASE TABLE'"
Write-Host "BASE TABLE count: source=$srcTables target=$dstTables"

$checkTables = @(
    "basket_member_account",
    "basket_member_profile",
    "basket_team",
    "basket_series",
    "basket_season",
    "basket_division",
    "basket_match"
)

$mismatch = 0
foreach ($t in $checkTables) {
    $srcCnt = Get-Scalar $SrcHost $SrcPort $SrcUser $SrcPass "SELECT COUNT(*) FROM ``$SrcDb``.``$t``"
    $dstCnt = Get-Scalar $DstHost $DstPort $DstUser $DstPass "SELECT COUNT(*) FROM ``$DstDb``.``$t``"
    $srcMax = Get-Scalar $SrcHost $SrcPort $SrcUser $SrcPass "SELECT COALESCE(MAX(id),0) FROM ``$SrcDb``.``$t``"
    $dstMax = Get-Scalar $DstHost $DstPort $DstUser $DstPass "SELECT COALESCE(MAX(id),0) FROM ``$DstDb``.``$t``"
    $ok = ($srcCnt -eq $dstCnt) -and ($srcMax -eq $dstMax) -and ($null -ne $srcCnt)
    if (-not $ok) { $mismatch++ }
    $flag = if ($ok) { "OK" } else { "DIFF" }
    Write-Host ("[{0}] {1}: count {2}->{3}, max(id) {4}->{5}" -f $flag, $t, $srcCnt, $dstCnt, $srcMax, $dstMax)
}

Write-Host ""
if ($mismatch -gt 0) {
    Write-Host "DONE with $mismatch verification DIFF(s). Dump: $dumpFile" -ForegroundColor Yellow
    exit 2
}
Write-Host "DONE. Sampled checks passed. Dump: $dumpFile" -ForegroundColor Green
exit 0