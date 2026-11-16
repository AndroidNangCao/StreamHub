param(
    [Parameter(Mandatory = $true)]
    [string]$GoogleServicesJson,
    [string]$OutputFile
)
$ErrorActionPreference = 'Stop'
$configPath = (Resolve-Path -LiteralPath $GoogleServicesJson).Path
$config = Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
$client = @($config.client | Where-Object { $_.client_info.android_client_info.package_name -eq 'com.nhom02.streamwire' })
if ($client.Count -ne 1) { throw 'Expected exactly one Android client for com.nhom02.streamwire.' }
$apiKey = [string]$client[0].api_key[0].current_key
$appId = [string]$client[0].client_info.mobilesdk_app_id
$projectId = [string]$config.project_info.project_id
if ([string]::IsNullOrWhiteSpace($apiKey) -or [string]::IsNullOrWhiteSpace($appId) -or [string]::IsNullOrWhiteSpace($projectId)) {
    throw 'Missing api_key, mobilesdk_app_id or project_id in google-services.json.'
}
$taskRoot = Split-Path -Parent $PSScriptRoot
$destination = Join-Path $taskRoot 'app/src/main/res/values/firebase.xml'
if (-not [string]::IsNullOrWhiteSpace($OutputFile)) { $destination = [System.IO.Path]::GetFullPath($OutputFile) }
$document = New-Object System.Xml.XmlDocument
$declaration = $document.CreateXmlDeclaration('1.0', 'utf-8', $null)
[void]$document.AppendChild($declaration)
$resources = $document.CreateElement('resources')
[void]$document.AppendChild($resources)
foreach ($entry in @(@('firebase_api_key', $apiKey), @('firebase_app_id', $appId), @('firebase_project_id', $projectId))) {
    $item = $document.CreateElement('string')
    $item.SetAttribute('name', $entry[0])
    $item.SetAttribute('translatable', 'false')
    $item.InnerText = $entry[1]
    [void]$resources.AppendChild($item)
}
$document.Save($destination)
Write-Output "Firebase Android configuration written to $destination. Nothing was uploaded or deployed."
