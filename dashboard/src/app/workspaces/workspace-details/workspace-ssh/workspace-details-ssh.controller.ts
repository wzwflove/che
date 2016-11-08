/*
 * Copyright (c) 2015-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 */
import {CheWorkspace} from "../../../../components/api/che-workspace.factory";
import {CheNotification} from "../../../../components/notification/che-notification.factory";
import {CheSsh} from "../../../../components/api/che-ssh.factory";
'use strict';

/**
 * @ngdoc controller
 * @name workspace.details.controller:WorkspaceDetailsSSHCtrl
 * @description This class is handling the controller for details of workspace : section ssh
 * @author Florent Benoit
 */
export class WorkspaceDetailsSshCtrl {

  /**
   * Workspace.
   */
  private cheWorkspace: CheWorkspace;

  /**
   * SSH.
   */
  private cheSsh: CheSsh;

  /**
   * Notification.
   */
  private cheNotification: CheNotification;

  /**
   * Material Design Dialog Service
   */
  private $mdDialog: ng.material.IDialogService;

  /**
   * Angular Log service.
   */
  private $log: ng.ILogService;

  /**
   * Angular Q service.
   */
  private $q: ng.IQService;

  private $timeout : ng.ITimeoutService;

  private privateKey : string;
  private publicKey : string;

  private namespace : string;
  private workspaceName : string;
  private workspaceKey : string;
  private workspace : any;
  private workspaceId: string;

  private sshKeyPair : any;


  /**
   * Default constructor that is using resource
   * @ngInject for Dependency injection
   */
  constructor($route : ng.route.IRouteService, cheSsh: CheSsh, cheWorkspace: CheWorkspace, cheNotification, $mdDialog : ng.material.IDialogService, $log : ng.ILogService, $q : ng.IQService, $timeout : ng.ITimeoutService) {
    this.cheWorkspace = cheWorkspace;
    this.cheSsh = cheSsh;
    this.cheNotification = cheNotification;
    this.$mdDialog = $mdDialog;
    this.$log = $log;
    this.$q = $q;

    this.namespace = $route.current.params.namespace;
    this.workspaceName = $route.current.params.workspaceName;
    this.workspaceKey = this.namespace + ":" + this.workspaceName;

    this.updateData();

  }


  updateData() {
    this.workspace = this.cheWorkspace.getWorkspaceByName(this.namespace, this.workspaceName);
    this.workspaceId = this.workspace.id;

    // get ssh key
    this.cheSsh.fetchKey("workspace", this.workspaceId).finally(() => {
      this.sshKeyPair = this.cheSsh.getKey("workspace", this.workspaceId);

    });
  }

  /**
   * Remove the default workspace keypair
   */
  removeDefaultKey() {
    this.cheSsh.removeKey("workspace", this.workspaceId).then(
      () => {this.$timeout( this.updateData(), 3000)}
    );
  }

  /**
   * Generate a new default workspace keypair
   */
  generateDefaultKey() {
    this.cheSsh.generateKey("workspace", this.workspaceId).then(() => {this.$timeout( this.updateData(), 3000)});
  }



}
