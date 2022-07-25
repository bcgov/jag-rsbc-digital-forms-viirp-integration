pipeline {
    agent none
    options {
        disableResume()
    }
    stages {
        stage('Abort Previously Running Jobs') {
            steps {
                echo "Aborting all running jobs ..."
                
                // script {
                //     abortAllPreviousBuildInProgress(currentBuild)
                // }
            }
        }
        stage('Build and Deploy Inputs') {
           agent none
           steps {
               confirm_build()
           }
        }
        stage('Build') {
            agent { label 'master' }
            options { skipDefaultCheckout(false) }
            steps {
                // build_app()
                script {
                    // sh """
                    //     echo ${env.CHANGE_ID}
                    //     echo ${env.CHANGE_BRANCH}
                    //     echo ${env.CHANGE_TARGET}
                    // """
                    // oc login --token=sha256~yXZppgKJWNXXyMY8dBhPzKuDwV0c7qy5Zp-0J_AO1Pg --server=https://api.sandbox-m2.ll9k.p1.openshiftapps.com:6443
                    // oc projects

                    // cd openshift
                    // oc process -f api-build-1.yaml --param-file build-params.yml --param VERSION=build-${env.CHANGE_ID} --param SUFFIX=-build-${env.CHANGE_ID} --param SOURCE_REPOSITORY_REF=${env.CHANGE_BRANCH}
                    sh """
                    
                    oc get pods


                    """
                }

            }
        }
        stage('Deploy (PR)') {
            // agent { label 'deploy' }
            steps {
                echo "Deploying PR ..."
            }
        }
        stage('Deploy (DEV)') {
            // agent { label 'deploy' }
            // when {
            //     expression { return env.CHANGE_TARGET == 'master';}
            //     beforeInput true
            // }
            input {
                message "Should we continue with deployment to DEV?"
                ok "Yes!"
            }
            steps {
                echo "Deploying to DEV..."
            }
        }
        stage('Deploy (TEST)') {
            // agent { label 'deploy' }
            // when {
            //     expression { return env.CHANGE_TARGET == 'master';}
            //     beforeInput true
            // }
            input {
                message "Should we continue with deployment to TEST?"
                ok "Yes!"
            }
            steps {
                echo "Deploying ..."
            }
        }
        stage('Deploy (PROD)') {
            // agent { label 'deploy' }
            // when {
            //     expression { return env.CHANGE_TARGET == 'master';}
            //     beforeInput true
            // }
            input {
                message "Should we continue with deployment to PROD?"
                ok "Yes!"
            }
            steps {
                echo "Deploying ..."
            }
        }
    }
}






// ###################################pipeline scripts#########################

void confirm_build(){
  script {
      def INPUT_PARAMS
      if (env.CHANGE_TARGET == 'master' && env.CHANGE_BRANCH.indexOf('patch/') != 0) {
        INPUT_PARAMS = input message: 'Build and deploy the latest changes?', ok: 'Yes',
            parameters: [
                choice(name: 'AUTO_DEPLOY_TO', choices: ['PR','DEV','TEST'].join('\n'), description: 'Deploy to'),
                booleanParam(defaultValue: false, name: 'SKIP_DEV', description: 'Skip Dev Deployment'),
                booleanParam(defaultValue: true, name: 'RUN_TEST', description: 'Execute automated testing'),
                booleanParam(defaultValue: false, name: 'DEBUG_LOGGING', description: 'Enable debugging')]
      }
      else if (env.CHANGE_TARGET.indexOf('release') == 0 && env.CHANGE_BRANCH.indexOf('patch/') != 0) {
        INPUT_PARAMS = input message: 'Build and deploy the latest changes?', ok: 'Yes',
            parameters: [
                choice(name: 'AUTO_DEPLOY_TO', choices: ['PR','DEV','TEST'].join('\n'), description: 'Deploy to'),
                // booleanParam(defaultValue: false, name: 'SKIP_DEV', description: 'Skip Dev Deployment'),
                booleanParam(defaultValue: true, name: 'RUN_TEST', description: 'Execute automated testing'),
                booleanParam(defaultValue: false, name: 'DEBUG_LOGGING', description: 'Enable debugging')]
      }
      else {
        INPUT_PARAMS = input message: 'Build and deploy the latest changes?', ok: 'Yes',
            parameters: [
                choice(name: 'AUTO_DEPLOY_TO', choices: ['PR'], description: 'Deploy to'),
                booleanParam(defaultValue: true, name: 'RUN_TEST', description: 'Execute automated testing'),
                booleanParam(defaultValue: false, name: 'DEBUG_LOGGING', description: 'Enable debugging')]
      }

      // Capture the preference of whether to skip dev and stage deployments
      env.SKIP_DEV = INPUT_PARAMS.SKIP_DEV;
      env.SKIP_STAGE = INPUT_PARAMS.SKIP_STAGE;

      if (INPUT_PARAMS.AUTO_DEPLOY_TO == 'PR') {
          env.AUTO_DEPLOY_TO = '1'
      }
      else if (INPUT_PARAMS.AUTO_DEPLOY_TO == 'DEV') {
          env.AUTO_DEPLOY_TO = '2'
      }
      else if (INPUT_PARAMS.AUTO_DEPLOY_TO == 'STAGE') {
          env.AUTO_DEPLOY_TO = '3'
      }
      else if (INPUT_PARAMS.AUTO_DEPLOY_TO == 'TEST') {
          env.AUTO_DEPLOY_TO = '4'
      }

      env.RUN_TEST = INPUT_PARAMS.RUN_TEST

      if (INPUT_PARAMS.DEBUG_LOGGING) {
        // Set this parameter to DEBUG=* to add debug
        env.DEBUG_LOGGING = "DEBUG=*"
      } else {
        // Leave as empty string to disable DEBUG logging
        env.DEBUG_LOGGING = "DEBUG="
      }

      echo "Confirming build and deploy of branch ${env.CHANGE_BRANCH}. AUTO_DEPLOY_TO: ${INPUT_PARAMS.AUTO_DEPLOY_TO}, SKIP_STAGE: ${INPUT_PARAMS.SKIP_STAGE}, RUN_TEST: ${INPUT_PARAMS.RUN_TEST}, DEBUG_LOGGING: ${INPUT_PARAMS.DEBUG_LOGGING}."
  }
}


void build_app(){
  script {
    //   def filesInThisCommitAsString = sh(script:"git diff --name-only HEAD~1..HEAD | grep -v '^.jenkins/' || echo -n ''", returnStatus: false, returnStdout: true).trim()
    //   def hasChangesInPath = (filesInThisCommitAsString.length() > 0)
    //   echo "${filesInThisCommitAsString}"
    //   if (!currentBuild.rawBuild.getCauses()[0].toString().contains('UserIdCause') && !hasChangesInPath){
    //       currentBuild.rawBuild.delete()
    //       error("No changes detected in the path ('^.jenkins/')")
    //   }
  }
  echo "Building ..."
  sh "cd .pipeline && npm ci && ${DEBUG_LOGGING} npm run build -- --pr=${CHANGE_ID}"
}