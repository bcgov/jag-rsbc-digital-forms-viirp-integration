pipeline {
    agent none
    options {
        disableResume()
        timeout(time: 24, unit: 'HOURS')
    }
    stages {
        // stage('Abort Previously Running Jobs') {
        //     steps {
        //         echo "Aborting all running jobs ..."                
        //         script {
        //             abortAllPreviousBuildInProgress(currentBuild)
        //         }
        //     }
        // }
        stage('Build and Deploy Inputs') {
           agent none
           steps {
               confirm_build()
           }
        }
        stage('Build') {
            agent { label 'master' }
            options { skipDefaultCheckout(false) }
            when {
                expression { return env.SKIP_BUILD == "false";}
                beforeInput true
            }
            steps {
                // build_app()
                script {
                    sh """                    
                    cd openshift
                    oc process -f api-build.yml --param-file build-params.yml --param VERSION=build-${env.CHANGE_ID} --param SUFFIX=-build-${env.CHANGE_ID} --param SOURCE_REPOSITORY_REF=${env.CHANGE_BRANCH} | oc apply -f -
                    oc start-build vips-api-build --follow --wait
                    """
                }

            }
        }

        stage('Scale Down PR') {
            agent { label 'master' }
            when {
                allOf {
                     expression { env.SKIP_PR != "true" }
                 }
                beforeInput true
            }
            steps {
                echo "Checking existing PR.."
                script{
                    def PR_Deploy_STATUS = sh ( script: "cd openshift && oc get deploy -n c220ad-dev vips-api-deployment-pr-${env.CHANGE_ID} -o jsonpath='{.metadata.name}'", returnStatus: true )
                    if(PR_Deploy_STATUS==1){
                        echo "No existing PR environments to scale down!!"
                    }else{
                        sh """
                            oc scale -n c220ad-dev deploy/vips-api-deployment-pr-${env.CHANGE_ID} --replicas=0

                        """
                    }
                }
            }
        }

        
        stage('Deploy (PR)') {
            agent { label 'master' }
            when {
                allOf {
                     expression { env.SKIP_PR != "true" }
                 }
                beforeInput true
            }
            input {
                message "Should we continue with deployment to PR?"
                ok "Yes!"
            }
            steps {
                echo "Deploying PR ..."
                script {
                    sh """                    
                    cd openshift
                    oc process -f api-deploy.yml --param-file pr-deploy-params.yml --param SUFFIX=-pr-${env.CHANGE_ID} --param BUILD_VERSION=${env.CHANGE_ID} | oc apply -f -
                    oc rollout status -n c220ad-dev deploy/vips-api-deployment-pr-${env.CHANGE_ID}
                    """
                }
            }
        }

        
        stage('Deploy (DEV)') {
            agent { label 'master' }
            when {
                allOf {
                     expression { env.SKIP_DEV != "true" }
                     expression { env.CHANGE_TARGET == 'master' }
                 }
                beforeInput true
            }
            input {
                message "Should we continue with deployment to DEV?"
                ok "Yes!"
            }
            
            stages{
                // stages{
                    stage('Scale down Dev'){
                    steps{
                        echo "Checking existing PR.."
                        script{
                            def DEV_Deploy_STATUS = sh ( script: "cd openshift && oc get deploy -n c220ad-dev vips-api-deployment-dev-${env.CHANGE_ID} -o jsonpath='{.metadata.name}'", returnStatus: true )
                            if(PR_Deploy_STATUS==1){
                                echo "No existing PR environments to scale down!!"
                            }else{
                                sh """
                                oc scale -n c220ad-dev deploy/vips-api-deployment-dev-${env.CHANGE_ID} --replicas=0
                                """
                            }
                        }
                    }
                    }

                    stage('Deploy (Dev)'){
                        steps{
                            echo 'deploying to dev'
                        }
                    }
                // }                
            }
        }




        stage('Deploy (TEST)') {
            agent { label 'master' }
            when {
                allOf {
                     expression { env.SKIP_TEST != "true" }
                     expression { env.CHANGE_TARGET == 'master' }
                 }
                beforeInput true
            }
            input {
                message "Should we continue with deployment to TEST?"
                ok "Yes!"
            }
            steps {
                echo "Deploying ..."
            }
        }
        stage('Deploy (PROD)') {
            agent { label 'master' }
            when {
                allOf {
                     expression { env.SKIP_PROD != "true" }
                     expression { env.CHANGE_TARGET == 'master' }
                 }
                beforeInput true
            }
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
                // choice(name: 'AUTO_DEPLOY_TO', choices: ['PR','DEV','TEST'].join('\n'), description: 'Deploy to'),
                booleanParam(defaultValue: false, name: 'SKIP_PR', description: 'Skip PR Deployment'),
                booleanParam(defaultValue: false, name: 'SKIP_DEV', description: 'Skip Dev Deployment'),
                booleanParam(defaultValue: false, name: 'SKIP_TEST', description: 'Skip Test Deployment'),
                booleanParam(defaultValue: false, name: 'SKIP_PROD', description: 'Skip Prod Deployment'),
                booleanParam(defaultValue: false, name: 'SKIP_BUILD', description: 'Skip Build Step?'),
                booleanParam(defaultValue: true, name: 'RUN_TEST', description: 'Execute automated testing')]
        env.SKIP_PR = INPUT_PARAMS.SKIP_PR;
        env.SKIP_DEV = INPUT_PARAMS.SKIP_DEV;
        env.SKIP_PROD = INPUT_PARAMS.SKIP_PROD;
        env.SKIP_TEST = INPUT_PARAMS.TEST;
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
                // choice(name: 'AUTO_DEPLOY_TO', choices: ['PR'], description: 'Deploy to'),
                booleanParam(defaultValue: false, name: 'SKIP_PR', description: 'Skip PR Deployment'),
                booleanParam(defaultValue: true, name: 'RUN_TEST', description: 'Execute automated testing'),
                booleanParam(defaultValue: false, name: 'SKIP_BUILD', description: 'Skip Build Step?')]
        env.SKIP_PR = INPUT_PARAMS.SKIP_PR;
                // env.SKIP_DEV="true"
                // env.SKIP_TEST ="true"
      }

      // Capture the preference of whether to skip dev and stage deployments
    //   env.SKIP_DEV = INPUT_PARAMS.SKIP_DEV;
    //   env.SKIP_STAGE = INPUT_PARAMS.SKIP_STAGE;
    //   env.SKIP_TEST = INPUT_PARAMS.TEST;
      env.SKIP_BUILD = INPUT_PARAMS.SKIP_BUILD;

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

      echo "Confirming build and deploy of branch ${env.CHANGE_BRANCH}. AUTO_DEPLOY_TO: ${INPUT_PARAMS.AUTO_DEPLOY_TO}, SKIP_BUILD: ${INPUT_PARAMS.SKIP_BUILD}, RUN_TEST: ${INPUT_PARAMS.RUN_TEST}, DEBUG_LOGGING: ${INPUT_PARAMS.DEBUG_LOGGING}."
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
