package registration_gate

import (
	controllerv1 "buf.build/gen/go/simplecloud/proto-specs/protocolbuffers/go/simplecloud/controller/v1"
	"context"
	"fmt"
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

		if pubSubHost == "" || pubSubPort == "" || pubSubSecret == "" {
			log.Error(nil, "Missing required environment variables")
			return nil
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

				// TODO:
				// api.getServers().updateServerProperty(event.serverAfter.uniqueId, "server-registered", "true")
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

		return nil
	},
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
