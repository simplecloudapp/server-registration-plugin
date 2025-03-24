package pkg

import (
	"buf.build/gen/go/simplecloud/proto-specs/grpc/go/simplecloud/controller/v1/controllerv1grpc"
	controllerv1 "buf.build/gen/go/simplecloud/proto-specs/protocolbuffers/go/simplecloud/controller/v1"
	"context"
	"fmt"
	"google.golang.org/grpc"
	"net"
	"os"

	pubsubv1 "buf.build/gen/go/simplecloud/proto-specs/protocolbuffers/go/simplecloud/pubsub/v1"
	"github.com/go-logr/logr"
	"go.minekube.com/gate/pkg/edition/java/proxy"

	pubsub "github.com/simplecloudapp/pubsub/go"
)

type ServerInfo struct {
	name    string
	address net.Addr
}

func (c *ServerInfo) Name() string {
	return c.name
}

func (c *ServerInfo) Addr() net.Addr {
	return c.address
}

var Plugin = proxy.Plugin{
	Name: "server-registration-plugin",
	Init: func(ctx context.Context, p *proxy.Proxy) error {
		log := logr.FromContextOrDiscard(ctx)
		log.Info("Started simplecloud server-registration-plugin")

		pubSubHost := os.Getenv("CONTROLLER_PUBSUB_HOST")

		pubSubPort := os.Getenv("CONTROLLER_PUBSUB_PORT")

		pubSubSecret := os.Getenv("CONTROLLER_SECRET")

		controllerHost := os.Getenv("CONTROLLER_HOST")
		controllerPort := os.Getenv("CONTROLLER_PORT")

		if pubSubHost == "" || pubSubPort == "" || pubSubSecret == "" {
			return fmt.Errorf("missing required environment variables")
		}

		err := initServers(p, controllerHost, controllerPort, pubSubSecret, log)

		if err != nil {
			return err
		}

		client, err := pubsub.NewPubSubClient(fmt.Sprintf("%s:%s", pubSubHost, pubSubPort), pubSubSecret)

		if err != nil {
			log.Error(err, "Failed to create pubsub client")
			return err
		}

		client.Subscribe("event", func(msg *pubsubv1.Message) {
			if msg.MessageBody == nil || msg.MessageBody.MessageData == nil {
				return
			}

			var updateEvent controllerv1.ServerUpdateEvent

			if err := msg.MessageBody.MessageData.UnmarshalTo(&updateEvent); err != nil {
				return
			}

			if updateEvent.ServerAfter.ServerType != controllerv1.ServerType_SERVER {
				return
			}

			if updateEvent.ServerAfter.ServerState == controllerv1.ServerState_AVAILABLE && updateEvent.ServerBefore.ServerState != controllerv1.ServerState_AVAILABLE {
				_, err := p.Register(buildServerInfo(updateEvent.ServerAfter))

				if err != nil {
					log.Error(err, "Failed to register server")
				} else {
					log.Info("Registered server", "server", updateEvent.ServerAfter)
				}

				grpcClient, c, err := getServerClient(controllerHost, controllerPort, pubSubSecret)
				defer c.Close()

				if err != nil {
					log.Error(err, "Failed to get server client")
					return
				}

				_, err = grpcClient.UpdateServerProperty(context.Background(), &controllerv1.UpdateServerPropertyRequest{
					ServerId:      updateEvent.ServerAfter.UniqueId,
					PropertyKey:   "server-registered",
					PropertyValue: "true",
				})

				if err != nil {
					log.Error(err, "Failed to update server property")
				}
			}
		})

		client.Subscribe("event", func(msg *pubsubv1.Message) {
			if msg.MessageBody == nil || msg.MessageBody.MessageData == nil {
				return
			}

			var stopEvent controllerv1.ServerStopEvent

			if err := msg.MessageBody.MessageData.UnmarshalTo(&stopEvent); err != nil {
				return
			}

			p.Unregister(buildServerInfo(stopEvent.Server))

			log.Info("Removed server", "server", stopEvent.Server)
		})

		log.Info("Subscribed to pubsub events")

		return nil
	},
}

func getServerClient(host string, port string, secret string) (controllerv1grpc.ControllerServerServiceClient, *grpc.ClientConn, error) {
	if host == "" {
		host = "127.0.0.1"
	}

	if port == "" {
		port = "5816"
	}

	connection := pubsub.CreateConnection(fmt.Sprintf("%s:%s", host, port), secret)

	grpcClient := controllerv1grpc.NewControllerServerServiceClient(connection)

	return grpcClient, connection, nil

}

func initServers(p *proxy.Proxy, host string, port string, secret string, log logr.Logger) error {

	grpcClient, c, err := getServerClient(host, port, secret)

	defer c.Close()

	servers, err := grpcClient.GetServersByType(context.Background(), &controllerv1.ServerTypeRequest{
		ServerType: controllerv1.ServerType_SERVER,
	})

	if err != nil {
		return err
	}

	for _, server := range servers.Servers {
		if server.ServerState != controllerv1.ServerState_AVAILABLE {
			continue
		}
		_, err := p.Register(buildServerInfo(server))

		if err != nil {
			log.Error(err, "Failed to register server")
		} else {
			log.Info("Registered server", "server", server)

			_, err = grpcClient.UpdateServerProperty(context.Background(), &controllerv1.UpdateServerPropertyRequest{
				ServerId:      server.UniqueId,
				PropertyKey:   "server-registered",
				PropertyValue: "true",
			})
		}
	}

	return nil
}

func buildServerInfo(server *controllerv1.ServerDefinition) *ServerInfo {
	ip := server.ServerIp
	port := server.ServerPort

	serverAddr, err := net.ResolveTCPAddr("tcp", fmt.Sprintf("%s:%d", ip, port))
	if err != nil {
		return nil
	}

	return &ServerInfo{
		name:    fmt.Sprintf("%s-%d", server.GroupName, server.NumericalId),
		address: serverAddr,
	}
}
