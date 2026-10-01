# Corre los tests de jcstress. Uso (desde la raíz del repo):
#   .\scripts\jcstress.ps1                 # modo quick sobre wsp.f2.*
#   .\scripts\jcstress.ps1 -Modo default   # más largo (el que va al README)
#   .\scripts\jcstress.ps1 -Tests "wsp.f2.jcstress.DosEncolan"
# Informe HTML en target\jcstress-results\index.html
#
# Compila con el perfil jcstress, que usa su propio directorio (target\jcstress-build) para
# que el IDE no vea el código generado. Ver el comentario del perfil en pom.xml.
param(
    [ValidateSet("sanity", "quick", "default", "tough")] [string] $Modo = "quick",
    [string] $Tests = "wsp.f2.*"
)

$ErrorActionPreference = "Stop"
$build = "target/jcstress-build"
mvn -q -Pjcstress clean test-compile
mvn -q -Pjcstress dependency:build-classpath "-Dmdep.outputFile=$build/cp.txt" "-Dmdep.includeScope=test"
$deps = Get-Content "$build/cp.txt" -Raw
$cp = "$build/classes;$build/test-classes;$deps"
if (Test-Path target/jcstress-results) { Remove-Item -Recurse -Force target/jcstress-results }
java -cp $cp org.openjdk.jcstress.Main -m $Modo -t $Tests -r target/jcstress-results
