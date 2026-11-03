// Esteira unica, com um estagio por gate e os componentes Python em paralelo (ADR-006).
// Qualquer gate que falhar interrompe o build, e o jar so e arquivado se todos passarem.
// Funciona em agente Windows (bat) e Unix (sh). O Python vem de PYTHON_CMD, por padrao "py" no Windows.

def mvn(String goals) {
    if (isUnix()) {
        sh "chmod +x mvnw && ./mvnw -B -ntp ${goals}"
    } else {
        bat "call mvnw.cmd -B -ntp ${goals}"
    }
}

def pytest(String pasta) {
    dir(pasta) {
        if (isUnix()) {
            sh """
                ${env.PYTHON_CMD ?: 'python3'} -m venv .venv
                .venv/bin/python -m pip install -q -r requirements-dev.txt
                .venv/bin/python -m pytest --junitxml=pytest-report.xml
            """
        } else {
            bat """
                ${env.PYTHON_CMD ?: 'py'} -m venv .venv || exit /b 1
                .venv\\Scripts\\python -m pip install -q -r requirements-dev.txt || exit /b 1
                .venv\\Scripts\\python -m pytest --junitxml=pytest-report.xml
            """
        }
    }
}

pipeline {
    agent any

    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                script { mvn('clean compile') }
            }
        }

        // Antes dos demais testes: uma violacao de arquitetura falha em segundos.
        stage('ArchUnit') {
            steps {
                script { mvn('test -Dtest=ModularArchitectureTest') }
            }
        }

        stage('Testes') {
            steps {
                script { mvn('test') }
            }
        }

        stage('Gate JaCoCo') {
            steps {
                script { mvn('jacoco:check@check') }
            }
        }

        stage('Testes Python') {
            parallel {
                stage('Servico de IA') {
                    steps {
                        script { pytest('labs/desafio-3/ia-service') }
                    }
                }
                stage('Simulador IoT') {
                    steps {
                        script { pytest('labs/desafio-5/simulador') }
                    }
                }
            }
        }

        stage('Arquivar artefatos') {
            steps {
                script { mvn('package -DskipTests') }
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
    }

    post {
        always {
            junit testResults: 'target/surefire-reports/*.xml, labs/**/pytest-report.xml', allowEmptyResults: true
            archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true
        }
    }
}
