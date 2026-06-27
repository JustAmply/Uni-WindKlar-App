param(
    [string]$Serial = "",
    [string]$OutputDir = "",
    [switch]$Build,
    [switch]$Install,
    [switch]$CleanAppData,
    [switch]$FullPage,
    [int]$FullPageScrolls = 14,
    [int]$InitialWaitSeconds = 8,
    [int]$PostOnboardingWaitSeconds = 15,
    [int]$StepWaitMilliseconds = 1200,
    [string]$PackageName = "product.lifecycle.windenergy",
    [string]$MainActivity = "product.lifecycle.windenergy.MainActivity"
)

$ErrorActionPreference = "Stop"
$script:AdbCommand = "adb"

function Write-Step {
    param([string]$Message)
    Write-Host "[screenshots] $Message"
}

function Get-RepoRoot {
    $scriptDir = Split-Path -Parent $PSCommandPath
    return (Resolve-Path (Join-Path $scriptDir "..")).Path
}

function Invoke-Adb {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $adbArgs = @()
    if ($Serial -ne "") {
        $adbArgs += @("-s", $Serial)
    }
    $adbArgs += $Arguments

    & $script:AdbCommand @adbArgs
    if ($LASTEXITCODE -ne 0) {
        throw "adb failed: $script:AdbCommand $($adbArgs -join ' ')"
    }
}

function Invoke-AdbText {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $adbArgs = @()
    if ($Serial -ne "") {
        $adbArgs += @("-s", $Serial)
    }
    $adbArgs += $Arguments

    $output = & $script:AdbCommand @adbArgs
    if ($LASTEXITCODE -ne 0) {
        throw "adb failed: $script:AdbCommand $($adbArgs -join ' ')"
    }
    return $output
}

function Resolve-AdbCommand {
    param([string]$RepoRoot)

    $pathAdb = Get-Command "adb" -ErrorAction SilentlyContinue
    if ($pathAdb) {
        return $pathAdb.Source
    }

    $candidates = New-Object System.Collections.Generic.List[string]
    $localPropertiesPath = Join-Path $RepoRoot "local.properties"

    if (Test-Path $localPropertiesPath) {
        $sdkLine = Get-Content $localPropertiesPath | Where-Object { $_ -match "^sdk\.dir=" } | Select-Object -First 1
        if ($sdkLine) {
            $sdkDir = ($sdkLine -replace "^sdk\.dir=", "") -replace "\\:", ":" -replace "\\\\", "\"
            $candidates.Add((Join-Path $sdkDir "platform-tools\adb.exe"))
        }
    }

    if ($env:ANDROID_HOME) {
        $candidates.Add((Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"))
    }

    if ($env:ANDROID_SDK_ROOT) {
        $candidates.Add((Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe"))
    }

    if ($env:LOCALAPPDATA) {
        $candidates.Add((Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"))
    }

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path $candidate)) {
            return (Resolve-Path $candidate).Path
        }
    }

    throw "Required command 'adb' was not found. Install Android platform-tools or add adb to PATH."
}

function Get-ConnectedDevice {
    $adbArgs = @()
    if ($Serial -ne "") {
        $adbArgs += @("-s", $Serial)
    }
    $adbArgs += "get-state"

    $state = (& $script:AdbCommand @adbArgs) -join ""
    if ($LASTEXITCODE -ne 0 -or $state.Trim() -ne "device") {
        throw "No ready Android device found. Start an emulator or connect a device, then run 'adb devices'."
    }
}

function Get-ScreenSize {
    $sizeOutput = Invoke-AdbText -Arguments @("shell", "wm", "size")
    $line = ($sizeOutput | Select-String -Pattern "Physical size|Override size" | Select-Object -Last 1).Line
    if ($line -notmatch "(\d+)x(\d+)") {
        throw "Could not read device screen size from 'adb shell wm size'."
    }

    return @{
        Width = [int]$Matches[1]
        Height = [int]$Matches[2]
    }
}

function Tap-Relative {
    param(
        [hashtable]$Screen,
        [double]$XRatio,
        [double]$YRatio,
        [string]$Label
    )

    $x = [math]::Round($Screen.Width * $XRatio)
    $y = [math]::Round($Screen.Height * $YRatio)
    Write-Step "Tap $Label at $x,$y"
    Invoke-Adb -Arguments @("shell", "input", "tap", "$x", "$y")
    Start-Sleep -Milliseconds $StepWaitMilliseconds
}

function Capture-Screen {
    param(
        [string]$Name,
        [string]$OutputDirectory
    )

    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $fileName = "$timestamp-$Name.png"
    $remotePath = "/sdcard/windklar-$fileName"
    $localPath = Join-Path $OutputDirectory $fileName

    Write-Step "Capture $Name"
    Invoke-Adb -Arguments @("shell", "screencap", "-p", $remotePath)
    Invoke-Adb -Arguments @("pull", $remotePath, $localPath)
    Invoke-Adb -Arguments @("shell", "rm", $remotePath)

    return $localPath
}

function Scroll-Down {
    param([hashtable]$Screen)

    $x = [math]::Round($Screen.Width * 0.50)
    $startY = [math]::Round($Screen.Height * 0.72)
    $endY = [math]::Round($Screen.Height * 0.42)
    Invoke-Adb -Arguments @("shell", "input", "swipe", "$x", "$startY", "$x", "$endY", "700")
    Start-Sleep -Milliseconds $StepWaitMilliseconds
}

function New-CroppedBitmap {
    param(
        [System.Drawing.Bitmap]$Bitmap,
        [int]$CropTopPx,
        [int]$CropBottomPx
    )

    $cropTop = [math]::Max(0, [math]::Min($CropTopPx, $Bitmap.Height - 1))
    $cropBottom = [math]::Max(0, [math]::Min($CropBottomPx, $Bitmap.Height - $cropTop - 1))
    $cropHeight = [math]::Max(1, $Bitmap.Height - $cropTop - $cropBottom)

    $source = New-Object System.Drawing.Rectangle(0, $cropTop, $Bitmap.Width, $cropHeight)
    $cropped = New-Object System.Drawing.Bitmap($Bitmap.Width, $cropHeight)
    $graphics = [System.Drawing.Graphics]::FromImage($cropped)
    try {
        $graphics.DrawImage(
            $Bitmap,
            (New-Object System.Drawing.Rectangle(0, 0, $Bitmap.Width, $cropHeight)),
            $source,
            [System.Drawing.GraphicsUnit]::Pixel
        )
    } finally {
        $graphics.Dispose()
    }

    return $cropped
}

function Get-OverlapDifference {
    param(
        [System.Drawing.Bitmap]$Previous,
        [System.Drawing.Bitmap]$Next,
        [int]$OverlapPx
    )

    $sampleXStep = [math]::Max(24, [math]::Floor($Previous.Width / 14))
    $sampleYStep = [math]::Max(24, [math]::Floor($OverlapPx / 18))
    $previousStartY = $Previous.Height - $OverlapPx
    $sum = 0.0
    $count = 0

    for ($y = 0; $y -lt $OverlapPx; $y += $sampleYStep) {
        for ($x = 0; $x -lt $Previous.Width; $x += $sampleXStep) {
            $a = $Previous.GetPixel($x, $previousStartY + $y)
            $b = $Next.GetPixel($x, $y)
            $sum += [math]::Abs($a.R - $b.R)
            $sum += [math]::Abs($a.G - $b.G)
            $sum += [math]::Abs($a.B - $b.B)
            $count += 3
        }
    }

    if ($count -eq 0) {
        return [double]::MaxValue
    }

    return $sum / $count
}

function Find-BestVerticalOverlap {
    param(
        [System.Drawing.Bitmap]$Previous,
        [System.Drawing.Bitmap]$Next
    )

    $maxOverlap = [math]::Min($Previous.Height, $Next.Height)
    $minOverlap = [math]::Min(160, $maxOverlap)
    if ($maxOverlap -le 1) {
        return 0
    }

    $bestOverlap = 0
    $bestScore = [double]::MaxValue
    $overlapStep = 16

    for ($overlap = $maxOverlap; $overlap -ge $minOverlap; $overlap -= $overlapStep) {
        $score = Get-OverlapDifference -Previous $Previous -Next $Next -OverlapPx $overlap
        $isMeaningfullyBetter = $score -lt ($bestScore - 0.75)
        $isSimilarButLarger = [math]::Abs($score - $bestScore) -le 0.75 -and $overlap -gt $bestOverlap

        if ($isMeaningfullyBetter -or $isSimilarButLarger) {
            $bestScore = $score
            $bestOverlap = $overlap
        }
    }

    if ($bestScore -gt 18.0) {
        Write-Step "No strong visual overlap found (score $([math]::Round($bestScore, 2))). Falling back to fixed crop."
        return 0
    }

    return $bestOverlap
}

function Add-BitmapBelow {
    param(
        [System.Drawing.Bitmap]$Top,
        [System.Drawing.Bitmap]$Bottom,
        [int]$OverlapPx
    )

    $safeOverlap = [math]::Max(0, [math]::Min($OverlapPx, $Bottom.Height))
    $appendHeight = $Bottom.Height - $safeOverlap
    if ($appendHeight -le 0) {
        return New-Object System.Drawing.Bitmap($Top)
    }

    $combined = New-Object System.Drawing.Bitmap($Top.Width, ($Top.Height + $appendHeight))
    $graphics = [System.Drawing.Graphics]::FromImage($combined)
    try {
        $graphics.Clear([System.Drawing.Color]::White)
        $graphics.DrawImage($Top, 0, 0)

        $source = New-Object System.Drawing.Rectangle(0, $safeOverlap, $Bottom.Width, $appendHeight)
        $destination = New-Object System.Drawing.Rectangle(0, $Top.Height, $Bottom.Width, $appendHeight)
        $graphics.DrawImage($Bottom, $destination, $source, [System.Drawing.GraphicsUnit]::Pixel)
    } finally {
        $graphics.Dispose()
    }

    return $combined
}

function Test-ScreenshotsVisuallySame {
    param(
        [string]$FirstPath,
        [string]$SecondPath
    )

    Add-Type -AssemblyName System.Drawing

    $first = $null
    $second = $null
    try {
        $first = New-Object System.Drawing.Bitmap($FirstPath)
        $second = New-Object System.Drawing.Bitmap($SecondPath)

        if ($first.Width -ne $second.Width -or $first.Height -ne $second.Height) {
            return $false
        }

        $top = [math]::Round($first.Height * 0.10)
        $bottom = [math]::Round($first.Height * 0.15)
        $sampleXStep = [math]::Max(24, [math]::Floor($first.Width / 14))
        $sampleYStep = [math]::Max(24, [math]::Floor(($first.Height - $top - $bottom) / 18))
        $sum = 0.0
        $count = 0

        for ($y = $top; $y -lt ($first.Height - $bottom); $y += $sampleYStep) {
            for ($x = 0; $x -lt $first.Width; $x += $sampleXStep) {
                $a = $first.GetPixel($x, $y)
                $b = $second.GetPixel($x, $y)
                $sum += [math]::Abs($a.R - $b.R)
                $sum += [math]::Abs($a.G - $b.G)
                $sum += [math]::Abs($a.B - $b.B)
                $count += 3
            }
        }

        if ($count -eq 0) {
            return $false
        }

        $averageDifference = $sum / $count
        return $averageDifference -lt 1.25
    } finally {
        if ($first) {
            $first.Dispose()
        }
        if ($second) {
            $second.Dispose()
        }
    }
}

function Join-ScreenshotsVertical {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$ImagePaths,
        [Parameter(Mandatory = $true)]
        [string]$OutputPath,
        [int]$TopCropPx,
        [int]$BottomCropPx
    )

    Add-Type -AssemblyName System.Drawing

    $loadedImages = New-Object System.Collections.Generic.List[System.Drawing.Bitmap]
    $croppedImages = New-Object System.Collections.Generic.List[System.Drawing.Bitmap]

    try {
        $totalImages = $ImagePaths.Count
        foreach ($path in $ImagePaths) {
            $bitmap = New-Object System.Drawing.Bitmap($path)
            $loadedImages.Add($bitmap)

            $index = $loadedImages.Count - 1
            $isOnlyImage = $totalImages -eq 1
            $cropTop = if ($index -eq 0) { 0 } else { $TopCropPx }
            $cropBottom = if ($isOnlyImage -or $index -eq ($totalImages - 1)) { 0 } else { $BottomCropPx }
            $croppedImages.Add((New-CroppedBitmap -Bitmap $bitmap -CropTopPx $cropTop -CropBottomPx $cropBottom))
        }

        $stitched = New-Object System.Drawing.Bitmap($croppedImages[0])
        try {
            for ($i = 1; $i -lt $croppedImages.Count; $i++) {
                $next = $croppedImages[$i]
                $previous = $croppedImages[$i - 1]
                $overlap = Find-BestVerticalOverlap -Previous $previous -Next $next
                Write-Step "Overlap part $i`: $overlap px"

                $combined = Add-BitmapBelow -Top $stitched -Bottom $next -OverlapPx $overlap
                $stitched.Dispose()
                $stitched = $combined
            }

            $stitched.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
        } finally {
            $stitched.Dispose()
        }
    } finally {
        foreach ($image in $croppedImages) {
            $image.Dispose()
        }
        foreach ($image in $loadedImages) {
            $image.Dispose()
        }
    }
}

function Capture-FullPage {
    param(
        [string]$Name,
        [string]$OutputDirectory,
        [hashtable]$Screen
    )

    $partsDir = Join-Path $OutputDirectory "parts\$Name"
    New-Item -ItemType Directory -Force -Path $partsDir | Out-Null

    $parts = New-Object System.Collections.Generic.List[string]
    $firstPart = Capture-Screen -Name "$Name-part-00" -OutputDirectory $partsDir
    $parts.Add($firstPart)

    $previousHash = (Get-FileHash -Algorithm SHA256 -Path $firstPart).Hash
    $reachedEnd = $false

    for ($i = 1; $i -le $FullPageScrolls; $i++) {
        Scroll-Down -Screen $Screen
        $partName = "{0}-part-{1:D2}" -f $Name, $i
        $partPath = Capture-Screen -Name $partName -OutputDirectory $partsDir
        $currentHash = (Get-FileHash -Algorithm SHA256 -Path $partPath).Hash

        if ($currentHash -eq $previousHash -or (Test-ScreenshotsVisuallySame -FirstPath $parts[$parts.Count - 1] -SecondPath $partPath)) {
            Remove-Item -LiteralPath $partPath -Force
            Write-Step "Reached end of $Name after $($parts.Count) part(s)"
            $reachedEnd = $true
            break
        }

        $parts.Add($partPath)
        $previousHash = $currentHash
    }

    if (-not $reachedEnd -and $parts.Count -gt 1) {
        Write-Step "Warning: $Name reached FullPageScrolls=$FullPageScrolls before a repeated end frame. Increase -FullPageScrolls if the bottom is missing."
    }

    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $stitchedPath = Join-Path $OutputDirectory "$timestamp-$Name-full.png"
    $topCropPx = [math]::Round($Screen.Height * 0.035)
    $bottomCropPx = [math]::Round($Screen.Height * 0.12)

    Write-Step "Stitch $Name from $($parts.Count) part(s)"
    Join-ScreenshotsVertical `
        -ImagePaths $parts.ToArray() `
        -OutputPath $stitchedPath `
        -TopCropPx $topCropPx `
        -BottomCropPx $bottomCropPx

    return $stitchedPath
}

function Capture-Page {
    param(
        [string]$Name,
        [string]$OutputDirectory,
        [hashtable]$Screen,
        [bool]$Scrollable
    )

    if ($FullPage -and $Scrollable) {
        return Capture-FullPage -Name $Name -OutputDirectory $OutputDirectory -Screen $Screen
    }

    return Capture-Screen -Name $Name -OutputDirectory $OutputDirectory
}

function Build-App {
    param([string]$RepoRoot)

    Write-Step "Build androidApp debug APK"
    Push-Location $RepoRoot
    try {
        & .\gradlew.bat :androidApp:assembleDebug
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle build failed."
        }
    } finally {
        Pop-Location
    }
}

function Install-App {
    param([string]$RepoRoot)

    $apkPath = Join-Path $RepoRoot "androidApp\build\outputs\apk\debug\androidApp-debug.apk"
    if (-not (Test-Path $apkPath)) {
        throw "Debug APK not found at '$apkPath'. Run this script with -Build first."
    }

    Write-Step "Install $apkPath"
    Invoke-Adb -Arguments @("install", "-r", $apkPath)
}

function Launch-App {
    Write-Step "Launch $PackageName/$MainActivity"
    Invoke-Adb -Arguments @(
        "shell",
        "am",
        "start",
        "-S",
        "-n",
        "$PackageName/$MainActivity"
    )
}

function Main {
    $repoRoot = Get-RepoRoot
    $script:AdbCommand = Resolve-AdbCommand -RepoRoot $repoRoot
    Write-Step "Use adb: $script:AdbCommand"

    if ($OutputDir -eq "") {
        $runName = Get-Date -Format "yyyyMMdd-HHmmss"
        $OutputDir = Join-Path $repoRoot "screenshots\android-ai\$runName"
    }

    New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

    if ($Build) {
        Build-App -RepoRoot $repoRoot
    }

    Get-ConnectedDevice

    if ($Install) {
        Install-App -RepoRoot $repoRoot
    }

    if ($CleanAppData) {
        Write-Step "Clear app data for deterministic first-run screenshots"
        Invoke-Adb -Arguments @("shell", "pm", "clear", $PackageName)
    }

    Invoke-Adb -Arguments @("shell", "svc", "power", "stayon", "true")
    Invoke-Adb -Arguments @("shell", "input", "keyevent", "KEYCODE_WAKEUP")
    Invoke-Adb -Arguments @("shell", "input", "keyevent", "KEYCODE_MENU")

    Launch-App
    Write-Step "Wait ${InitialWaitSeconds}s for app startup and local snapshot import"
    Start-Sleep -Seconds $InitialWaitSeconds

    $screen = Get-ScreenSize
    Write-Step "Device screen size: $($screen.Width)x$($screen.Height)"

    $captured = New-Object System.Collections.Generic.List[string]

    $captured.Add((Capture-Page -Name "00-start" -OutputDirectory $OutputDir -Screen $screen -Scrollable $false))

    Tap-Relative -Screen $screen -XRatio 0.82 -YRatio 0.105 -Label "Skip onboarding"
    Write-Step "Wait ${PostOnboardingWaitSeconds}s for map and snapshot data"
    Start-Sleep -Seconds $PostOnboardingWaitSeconds
    $captured.Add((Capture-Page -Name "01-map" -OutputDirectory $OutputDir -Screen $screen -Scrollable $false))

    $bottomNavY = 0.94
    $navItems = @(
        @{ Name = "02-stats"; X = 0.30; Label = "Stats"; Scrollable = $true },
        @{ Name = "03-favorites"; X = 0.50; Label = "Favorites"; Scrollable = $true },
        @{ Name = "04-faq"; X = 0.70; Label = "FAQ"; Scrollable = $true },
        @{ Name = "05-profile"; X = 0.90; Label = "Profile"; Scrollable = $true },
        @{ Name = "06-map-return"; X = 0.10; Label = "Map"; Scrollable = $false }
    )

    foreach ($item in $navItems) {
        Tap-Relative -Screen $screen -XRatio $item.X -YRatio $bottomNavY -Label $item.Label
        $captured.Add((Capture-Page -Name $item.Name -OutputDirectory $OutputDir -Screen $screen -Scrollable $item.Scrollable))
    }

    $manifestPath = Join-Path $OutputDir "manifest.txt"
    $manifest = @(
        "WindKlar Android screenshot run",
        "Created: $(Get-Date -Format o)",
        "Package: $PackageName",
        "Activity: $MainActivity",
        "Device: $((Invoke-AdbText -Arguments @('shell', 'getprop', 'ro.product.model')) -join ' ')",
        "Screen: $($screen.Width)x$($screen.Height)",
        "",
        "Files:"
    ) + ($captured | ForEach-Object { Split-Path -Leaf $_ })

    Set-Content -Path $manifestPath -Value $manifest -Encoding utf8

    Write-Step "Done. Wrote $($captured.Count) screenshots to:"
    Write-Host $OutputDir
}

Main
