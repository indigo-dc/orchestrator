<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dm="urn:jboss:domain:3.0"
				xmlns:log="urn:jboss:domain:logging:3.0"
				xmlns:scn="urn:jboss:domain:deployment-scanner:2.0"
				xmlns:jgs="urn:jboss:domain:jgroups:3.0"
				xmlns:weld="urn:jboss:domain:weld:2.0"
				xmlns:undertow="urn:jboss:domain:undertow:2.0">

    <xsl:output method="xml" indent="yes" />

	<xsl:variable name="jgroupsConf">
		<property name="log_discard_msgs" xmlns="urn:jboss:domain:jgroups:3.0">
            	false
        </property>
    </xsl:variable>

	<xsl:variable name="customLoggers">
		<logger category="org.drools.core.xml.ExtensibleXmlParser" xmlns="urn:jboss:domain:logging:3.0">
			<level name="FATAL" />
		</logger>
		<logger category="it.reply" xmlns="urn:jboss:domain:logging:3.0">
			<level name="DEBUG" />
		</logger>
	</xsl:variable>
	
	<xsl:variable name="deploymentLoggingConfig">
		<use-deployment-logging-config xmlns="urn:jboss:domain:logging:3.0" value="false"/>
	</xsl:variable>
   
   <xsl:variable name="jsonFormatter">
   			<formatter xmlns="urn:jboss:domain:logging:3.0" name="jsonFormatter">
				<custom-formatter module="biz.paluch.logging" class="biz.paluch.logging.gelf.wildfly.WildFlyJsonFormatter">
					<properties>
						<property name="version" value="1.1" />
						<property name="fields" value="Time,Severity,ThreadName,SourceClassName,SourceMethodName,SourceSimpleClassName,LoggerName,NDC" />
						<property name="extractStackTrace" value="true" />
						<property name="filterStackTrace" value="true" />
<!-- 					<property name="includeLogMessageParameters" value="false" /> -->
						<property name="mdcProfiling" value="false" />
						<property name="timestampPattern" value="yyyy-MM-dd HH:mm:ss,SSSS" />
						<property name="facility" value="orchestrator" />
						<property name="mdcFields" value="request_id" />
<!--					<property name="additionalFields" value="fieldName1=fieldValue1,fieldName2=fieldValue2" />
						<property name="additionalFieldTypes" value="fieldName1=String,fieldName2=Double,fieldName3=Long" />
						<property name="dynamicMdcFields" value="mdc.*,(mdc|MDC)fields" />
						<property name="includeFullMdc" value="true" /> -->
					</properties>
				</custom-formatter>
			</formatter>
   </xsl:variable>

<!--	<xsl:variable name="newPubInterface">
		<interface name="public"  xmlns="urn:jboss:domain:3.0">
            <any-ipv4-address />
        </interface>
    </xsl:variable>
	
		<xsl:variable name="newManagementInterface">
		<interface name="management"  xmlns="urn:jboss:domain:3.0">
            <any-ipv4-address />
        </interface>
    </xsl:variable>
	
    <xsl:template match="//dm:interfaces/dm:interface[@name='public']">
        <xsl:copy-of copy-namespaces="no" select="$newPubInterface"/>
    </xsl:template>

	<xsl:template match="//dm:interfaces/dm:interface[@name='management']">
        <xsl:copy-of copy-namespaces="no" select="$newManagementInterface"/>
    </xsl:template>
    
	 <xsl:template match="//scn:subsystem/scn:deployment-scanner">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()"/>
			<xsl:attribute name="deployment-timeout">300</xsl:attribute>
		</xsl:copy>
    </xsl:template> -->
    	<xsl:template match="//log:subsystem">
	    <xsl:copy>
	        <xsl:apply-templates select="@*"/>
		<xsl:copy-of copy-namespaces="no" select="$deploymentLoggingConfig"/>
		<xsl:apply-templates select="node()"/>    
	    </xsl:copy>
	</xsl:template>
	
	<xsl:template match="//log:subsystem/log:root-logger">
	    <xsl:copy-of copy-namespaces="no" select="$customLoggers"/>
	    <xsl:copy>
	        <xsl:apply-templates select="@*|node()"/>
	    </xsl:copy>
	    <xsl:copy-of copy-namespaces="no" select="$jsonFormatter"/>
	</xsl:template>

	<xsl:template match="//log:subsystem/log:console-handler[@name='CONSOLE']/log:level/@name">
		<xsl:attribute name="name">DEBUG</xsl:attribute>
	</xsl:template>
	
	<xsl:template match="//log:subsystem/log:console-handler[@name='CONSOLE']/log:formatter/log:named-formatter/@name">
		<xsl:attribute name="name">jsonFormatter</xsl:attribute>
	</xsl:template>
	
	<xsl:template match="//log:subsystem/log:periodic-rotating-file-handler[@name='FILE']">
		<xsl:copy>
			<xsl:attribute name="enabled">false</xsl:attribute>
			<xsl:apply-templates select="@*|node()"/>
		</xsl:copy>
    </xsl:template>

	<xsl:template match="//jgs:subsystem/jgs:stacks/jgs:stack[@name='udp']/jgs:transport[@type='UDP']">
		<xsl:copy>
			<xsl:copy-of select="@*" />
			<xsl:copy-of copy-namespaces="no" select="$jgroupsConf"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="//weld:subsystem">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()"/>
			<xsl:attribute name="require-bean-descriptor">true</xsl:attribute>
		</xsl:copy>
    </xsl:template>
    
   <xsl:template match="//undertow:subsystem/undertow:server[@name='default-server']/undertow:http-listener[@name='default']">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:attribute name="proxy-address-forwarding">true</xsl:attribute>
			<xsl:apply-templates select="node()"/>
		</xsl:copy>
    </xsl:template>
    
    <!-- Copy everything else. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
