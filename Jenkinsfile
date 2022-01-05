pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        git(url: 'https://github.com/Mine-Quest/Payload.git', branch: 'main', credentialsId: 'Iv1.88498de6720734a8')
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
