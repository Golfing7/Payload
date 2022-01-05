pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        git(url: 'https://github.com/Mine-Quest/Payload.git', branch: 'main', credentialsId: 'Iv1.203e0ccff150d046')
        sh 'mvn clean install'
      }
    }

    stage('Archive') {
      steps {
        archiveArtifacts(artifacts: 'target/Payload-3.1.0.jar', onlyIfSuccessful: true)
      }
    }

  }
}