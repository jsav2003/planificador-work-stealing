# Corre los tests de jcstress. Uso (desde la raíz del repo):
#   .\scripts\jcstress.ps1                 # modo quick sobre wsp.f2.*
#   .\scripts\jcstress.ps1 -Modo default   # más largo (el que va al README)
#   .\scripts\jcstress.ps1 -Tests "wsp.f2.jcstress.ColaMichaelScottStress.DosEncolan"
# Informe HTML en target\jcstress-results\index.html
param(
    [ValidateSet("sanity", "quick", "default", "tough")] [string] $Modo = "quick",
    [string] $Tests = "wsp.f2.*"
)

$ErrorActionPreference = "Stop"
mvn -q test-compile
mvn -q dependency:build-classpath "-Dmdep.outputFile=target/cp.txt" "-Dmdep.includeScope=test"
$deps = Get-Content target/cp.txt -Raw
$cp = "target/classes;target/test-classes;$deps"
if (Test-Path target/jcstress-results) { Remove-Item -Recurse -Force target/jcstress-results }
java -cp $cp org.openjdk.jcstress.Main -m $Modo -t $Tests -r target/jcstress-results
