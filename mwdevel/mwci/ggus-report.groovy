#!/usr/bin/env groovy
pipeline {
 
  agent { label 'docker' }
 
  options {
    timeout(time: 10, unit: 'MINUTES')
    buildDiscarder(logRotator(numToKeepStr: '5'))
    disableConcurrentBuilds()
  }

  triggers { cron('H/20 * * * *') }

  environment {
    REPORT_DIR = "/tmp/${env.BUILD_TAG}/reports"
  }

  stages {
    stage('run'){
      steps {
          sh "mkdir -p ${env.REPORT_DIR}"
          sh "docker run --name ggus-report-${env.BUILD_NUMBER} -v ${env.REPORT_DIR}:/tmp/reports -e \"REPORT_DIR=${REPORT_DIR}\" italiangrid/ggus-mon:latest"
      }
    }

    stage('archive & clean'){
      steps {
	      dir("reports"){ 
	        archiveArtifacts "/tmp/${env.BUILD_TAG}/reports" 
	      }
      }
    }
  }

  post {
    failure {
      slackSend color: 'danger', message: "${env.JOB_NAME} - #${env.BUILD_NUMBER} Failure (<${env.BUILD_URL}|Open>)"
    }

    changed {
      script{
        if('SUCCESS'.equals(currentBuild.currentResult)) {
          slackSend color: 'good', message: "${env.JOB_NAME} - #${env.BUILD_NUMBER} Back to normal (<${env.BUILD_URL}|Open>)"
        }
      }
    }
  }
}
