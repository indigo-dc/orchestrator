package main

import (
	"os"
	"bufio"
	"fmt"
	"net/http"
	"net/url"
	"crypto/tls"
	"strings"
	"github.com/dghubble/sling"
)

type OrchentError struct {
	Code int `json:"code"`
	Title string `json:"title"`
	Message string `json:"message"`
}

type OrchentError2 struct {
	Title string `json:"error"`
	Message string `json:"error_description"`
}

func (e OrchentError2) Error() string {
	return fmt.Sprintf("Error '%s': %s", e.Title, e.Message)
}

func (e OrchentError) Error() string {
	return fmt.Sprintf("Error [%d] '%s': %s", e.Code, e.Title, e.Message)
}

type OrchentLink  struct {
	Rel string `json:"rel"`
	HRef string `json:"href"`
}

type OrchentPage  struct {
	Size int `json:"size"`
	TotalElements int `json:"totalElements"`
	TotalPages int `json:"totalPages"`
	Number int `json:"number"`
}

type OrchentDeployment struct {
	Uuid string `json:"uuid"`
	CreationTime string `json:"creationTime"`
	UpdateTime string `json:"updateTime"`
	Status string `json:"status"`
	Task string `json:"task"`
	Callback string `json:"callback"`
	Output map[string]interface{} `json:"output"`
	Links []OrchentLink `json:"links"`
}

type OrchentResource struct {
	Uuid string `json:"uuid"`
	CreationTime string `json:"creationTime"`
	State string `json:"state"`
	ToscaNodeType string `json:"toscaNodeType"`
	ToscaNodeName string `json:"toscaNodeName"`
	RequiredBy []string `json:"requiredBy"`
	Links []OrchentLink `json:"links"`
}

type OrchentDeploymentList struct {
	Deployments  []OrchentDeployment `json:"content"`
	Links []OrchentLink `json:"links"`
	Page OrchentPage `json:"page"`
}

type OrchentResourceList struct {
	Resources  []OrchentResource `json:"content"`
	Links []OrchentLink `json:"links"`
	Page OrchentPage `json:"page"`
}

func (error OrchentError) String() (string) {
	return fmt.Sprintf("%s [%d]: %s ", error.Title, error.Code, error.Message)
}

func (error OrchentError2) String() (string) {
	return fmt.Sprintf("%s: %s ", error.Title, error.Message)
}


func (depList OrchentDeploymentList) String() (string) {
	output := ""
	output = output + fmt.Sprintln("list of deployments:")
	output = output + fmt.Sprintf("  page: %s\n",depList.Page)
	output = output + fmt.Sprintln("  links:")
	for _, link := range depList.Links {
		output = output + fmt.Sprintf("    %s\n",link)
	}
	output = output + fmt.Sprintln("\n")
	for _, dep := range depList.Deployments {
		output = output + fmt.Sprintln(dep)
	}
	return output
}

func (dep OrchentDeployment) String() (string) {
	lines := []string{"Deployment ["+dep.Uuid+"]:",
		"  status: "+dep.Status,
		"  creation time: "+dep.CreationTime,
		"  update time: "+dep.UpdateTime,
		"  callback: "+dep.Callback,
		"  output: "+fmt.Sprintf("%s",dep.Output),
		"  links:"}
	output := ""
	for _, line := range lines {
		output = output + fmt.Sprintf("%s\n", line)
	}
	for _, link := range dep.Links {
		output = output + fmt.Sprintf("    %s\n",link)
	}
	return output
}

func (resList OrchentResourceList) String() (string) {
	output := fmt.Sprintln("list of resources:")
	for _, res := range resList.Resources {
		output = output + fmt.Sprintln(res)
	}
	return output
}

func (res OrchentResource) String() (string) {
	lines := []string{"Resource ["+res.Uuid+"]:",
		"  creation time: "+res.CreationTime,
		"  state: "+res.State,
		"  toscaNodeType: "+res.ToscaNodeType,
		"  toscaNodeName: "+res.ToscaNodeName,
		"  requiredBy:"}
	output := ""
	for _, line := range lines {
		output = output + fmt.Sprintf("%s\n", line)
	}
	for _, req := range res.RequiredBy {
		output = output + fmt.Sprintf("    %s\n", req)
	}
	output = output + "  links:\n"
	for _, link := range res.Links {
		output = output + fmt.Sprintf("    %s\n",link)
	}
	return output
}

func (link OrchentLink) String() (string) {
	return fmt.Sprintf("%s [%s]", link.Rel, link.HRef)
}

func (page OrchentPage) String() (string) {
	return fmt.Sprintf("%d/%d [ #Elements: %d, size: %d ]", page.Number, page.TotalPages, page.TotalElements, page.Size)
}


func show_help() {
	fmt.Println("Usage:")
	fmt.Println("  # please export your access token in the environment variable " )
	fmt.Println("  # called ORCHENT_TOKEN, so you do not need to type it everytime " )
	fmt.Println("  # export ORCHENT_TOKEN=<my access token> " )
	fmt.Println("  " )
	fmt.Println("  # List deployments")
	fmt.Println("    <base url> depls")
	fmt.Println("  # Show a specific deployment")
	fmt.Println("    <base url> depshow <dep uuid>")
	fmt.Println("  # Delete a specific deployment")
	fmt.Println("    <base url> depdel <dep uuid>")
	fmt.Println("  # List resources of a deployment")
	fmt.Println("    <base url> resls <dep uuid>")
	fmt.Println("  # show a specific resource of a deployment")
	fmt.Println("    <base url> resshow <dep uuid>")
}


func client() (*http.Client) {
	_, set := os.LookupEnv("ORCHENT_INSECURE")
	if set {
		tr := &http.Transport{
			TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
		}
		return &http.Client{Transport: tr}
	} else {
		return http.DefaultClient
	}
}

func deployments_list(base *sling.Sling) {
	deploymentList := new(OrchentDeploymentList)
	orchentError := new(OrchentError2)
	base = base.Get("./deployments")
	_, err := base.Receive(deploymentList, orchentError)
	if err != nil {
		fmt.Printf("error requesting list of providers:\n %s\n",err)
		return
	}
	if orchentError.Title == "" && orchentError.Message == "" {
		fmt.Printf("%s\n", deploymentList)
	} else {
		fmt.Printf("error requesting list of deployments:\n %s\n", orchentError)
	}
}

func deployment_show(uuid string, base *sling.Sling) {
	deployment := new(OrchentDeployment)
	orchentError := new(OrchentError2)
	base = base.Get("./deployments/"+uuid)
	_, err := base.Receive(deployment, orchentError)
	if err != nil {
		fmt.Printf("error requesting provider %s:\n %s\n", uuid, err)
		return
	}
	if orchentError.Title == "" && orchentError.Message == "" {
		fmt.Printf("%s\n", deployment)
	} else {
		fmt.Printf("error requesting deployment %s:\n %s\n",uuid, orchentError)
	}
}

func deployment_get_template(uuid string, baseUrl string) {
	cl := client()
	tokenValue, tokenSet := os.LookupEnv("ORCHENT_TOKEN")
	req, _ := http.NewRequest("GET", baseUrl+"deployments/"+uuid+"/template", nil)
	token := "Bearer "+tokenValue
	if tokenSet {
		req.Header.Add("Authorization", token)
	}
	resp, err:= cl.Do(req)
	if err != nil {
		fmt.Printf("error requesting template of %s:\n  %s\n", uuid, err)
		return
	}
	// TODO: check return value
	defer resp.Body.Close()
	scanner := bufio.NewScanner(resp.Body)
	scanner.Split(bufio.ScanBytes)
	for scanner.Scan() {
		fmt.Print(scanner.Text())
	}
}

func deployment_delete(uuid string, baseUrl string) {
	cl := client()
	tokenValue, tokenSet := os.LookupEnv("ORCHENT_TOKEN")
	req, _ := http.NewRequest("DELETE", baseUrl+"deployments/"+uuid, nil)
	token := "Bearer "+tokenValue
	if tokenSet {
		req.Header.Add("Authorization", token)
	}
	_, err:= cl.Do(req)
	if err != nil {
		fmt.Printf("error deleting deployment %s:\n  %s\n", uuid, err)
		return
	}
	// TODO: check return value
	fmt.Printf("deployment deleted\n")
}

func resources_list(depUuid string, base *sling.Sling) {
	resourceList := new(OrchentResourceList)
	orchentError := new(OrchentError2)
	base = base.Get("./deployments/"+depUuid+"/resources")
	_, err := base.Receive(resourceList, orchentError)
	if err != nil {
		fmt.Printf("error requesting list of resources for %s:\n %s\n", depUuid, err)
		return
	}
	if orchentError.Title == "" && orchentError.Message == "" {
		fmt.Printf("%s\n", resourceList)
	} else {
		fmt.Printf("error requesting resource list for %s:\n %s\n", depUuid, orchentError)
	}
}

func resource_show(depUuid string, resUuid string, base *sling.Sling) {
	resource := new(OrchentResource)
	orchentError := new(OrchentError2)
	base = base.Get("./deployments/"+depUuid+"/resources/"+resUuid)
	_, err := base.Receive(resource, orchentError)
	if err != nil {
		fmt.Printf("error requesting resources %s for %s:\n %s\n", resUuid, depUuid, err)
		return
	}
	if orchentError.Title == "" && orchentError.Message == "" {
		fmt.Printf("%s\n", resource)
	} else {
		fmt.Printf("error requesting resource %s for %s:\n %s\n", resUuid, depUuid, orchentError)
	}
}

func list_services(host, token, issuer string) {
     fmt.Println("listing services")
}

func list_credentials(host, token, issuer string) {
     fmt.Println("listing credentials")
}

func request(host, serviceId, token, issuer string) {
     fmt.Println("requesting credential")
}

func revoke(host, credId, token, issuer string) {
     fmt.Println("revoking credential")
}



func base_connection(urlBase string) (*sling.Sling) {
	client := client()
	tokenValue, tokenSet := os.LookupEnv("ORCHENT_TOKEN")
	base := sling.New().Client(client).Base(urlBase)
	base = base.Set("User-Agent", "Orchent")
	base = base.Set("Accept", "application/json")
	if tokenSet {
		token := "Bearer "+tokenValue
		return base.Set("Authorization",token)
	} else {
		return base
	}
}

func base_url(rawUrl string) (string) {
	if ! strings.HasSuffix(rawUrl, "/") {
		rawUrl = rawUrl + "/"
	}
	u, _ := url.Parse(rawUrl)
	urlBase := u.Scheme + "://" + u.Host + u.Path
	return urlBase
}

func main() {
	args := os.Args[1:]
	argsNum := len(args)
	if argsNum < 2 {
		show_help()
		return
	}
	baseUrl := base_url(args[0])
	base := base_connection(baseUrl)
	cmd := args[1]

	switch cmd {
	case "depls":
		deployments_list(base)
	case "depshow":
		if argsNum == 3 {
			deployment_show(args[2], base)
		} else {
			show_help()
		}
	case "deptemplate":
		if argsNum == 3 {
			deployment_get_template(args[2], baseUrl)
		} else {
			show_help()
		}
	case "depdel":
		if argsNum == 3 {
			deployment_delete(args[2], baseUrl)
		} else {
			show_help()
		}
	case "resls":
		if argsNum == 3 {
			resources_list(args[2], base)
		} else {
			show_help()
		}
	case "resshow":
		if argsNum == 4 {
			resource_show(args[2], args[3], base)
		} else {
			show_help()
		}
	default:
		show_help()
	}
}
