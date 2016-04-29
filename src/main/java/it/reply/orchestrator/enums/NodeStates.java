package it.reply.orchestrator.enums;

/**
 * Normative nodes states. For more details see @see <a href=
 * "http://docs.oasis-open.org/tosca/TOSCA-Simple-Profile-YAML/v1.0/csprd02/TOSCA-Simple-Profile-YAML-v1.0-csprd02.html#_Toc445238244">
 * Tosca Simple Profile</a>
 * 
 */
public enum NodeStates {

  // @formatter:off

  // Node is not yet created. Node only exists as a template definition.
  INITIAL,

  // Node is transitioning from initial state to created state.
  CREATING,

  // Node software has been installed.
  CREATED,

  // Node is transitioning from created state to configured state.
  CONFIGURING,

  // Node has been configured prior to being started.
  CONFIGURED,

  // Node is transitioning from configured state to started state.
  STARTING,

  // Node is started.
  STARTED,

  // Node is transitioning from its current state to a configured state.
  STOPPING,

  // Node is transitioning from its current state to one where it is deleted and its state is no
  // longer tracked by the instance model.
  DELETING,

  // Node is in an error state.
  ERROR;
  // @formatter:on

}
